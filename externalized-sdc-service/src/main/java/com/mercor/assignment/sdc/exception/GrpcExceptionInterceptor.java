package com.mercor.assignment.sdc.exception;

import com.mercor.assignment.sdc.metrics.GrpcErrorMetrics;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Global exception interceptor for gRPC services.
 * Converts application exceptions to appropriate gRPC status codes.
 * Preserves detailed error metadata and adds correlation IDs for tracing.
 */
@Component
public class GrpcExceptionInterceptor implements ServerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(GrpcExceptionInterceptor.class);

    // Standard metadata keys for error details
    private static final Metadata.Key<String> ERROR_DETAILS_KEY = Metadata.Key.of("error-details",
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ERROR_CODE_KEY = Metadata.Key.of("error-code",
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ERROR_CAUSE_KEY = Metadata.Key.of("error-cause",
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> CORRELATION_ID_KEY = Metadata.Key.of("correlation-id",
            Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ERROR_LOCATION_KEY = Metadata.Key.of("error-location",
            Metadata.ASCII_STRING_MARSHALLER);

    private final GrpcErrorMetrics metrics;

    @Autowired
    public GrpcExceptionInterceptor(GrpcErrorMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        // Extract or generate correlation ID
        String correlationId = headers.get(CORRELATION_ID_KEY);
        if (correlationId == null) {
            correlationId = generateCorrelationId();
        }

        // Store the correlation ID for this request
        final String finalCorrelationId = correlationId;

        // Add correlation ID to MDC for logging
        MDC.put("correlation_id", finalCorrelationId);

        // Start timing the method call
        Timer.Sample timerSample = metrics.startTimer(call.getMethodDescriptor().getFullMethodName());

        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        return new ExceptionHandlingServerCallListener<>(
                listener, call, finalCorrelationId, timerSample);
    }

    private class ExceptionHandlingServerCallListener<ReqT, RespT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        private final ServerCall<ReqT, RespT> serverCall;
        private final String correlationId;
        private final Timer.Sample timerSample;

        ExceptionHandlingServerCallListener(
                ServerCall.Listener<ReqT> listener,
                ServerCall<ReqT, RespT> serverCall,
                String correlationId,
                Timer.Sample timerSample) {
            super(listener);
            this.serverCall = serverCall;
            this.correlationId = correlationId;
            this.timerSample = timerSample;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (EntityNotFoundException e) {
                logError("Entity not found", e);
                handleException(e, Status.NOT_FOUND, "Entity not found: " + e.getMessage(), e.getErrorCode());
            } catch (ValidationException e) {
                logError("Validation failed", e);
                String message = e.getMessage();
                if (e.hasFieldErrors()) {
                    message = formatValidationErrors(e);
                }
                handleException(e, Status.INVALID_ARGUMENT, message, e.getErrorCode());
            } catch (PermissionDeniedException e) {
                logError("Permission denied", e);
                handleException(e, Status.PERMISSION_DENIED, e.getMessage(), e.getErrorCode());
            } catch (SCDException e) {
                logError("Business logic exception", e);
                handleException(e, Status.INVALID_ARGUMENT, "Invalid request: " + e.getMessage(), e.getErrorCode());
            } catch (StatusRuntimeException e) {
                // Pass through existing gRPC StatusRuntimeException with all metadata
                logError("gRPC status error", e);
                metrics.recordError(e.getStatus().getCode(), "STATUS_RUNTIME_EXCEPTION",
                        serverCall.getMethodDescriptor().getFullMethodName());
                metrics.stopTimer(timerSample, serverCall.getMethodDescriptor().getFullMethodName(),
                        e.getStatus().getCode());
                serverCall.close(e.getStatus(), e.getTrailers() != null ? e.getTrailers() : new Metadata());
            } catch (IllegalArgumentException e) {
                logError("Invalid argument", e);
                handleException(e, Status.INVALID_ARGUMENT, "Invalid argument: " + e.getMessage(), "INVALID_ARGUMENT");
            } catch (Exception e) {
                logError("Unexpected error in gRPC call", e);
                // For unexpected errors, don't expose internal details
                handleException(e, Status.INTERNAL, "An internal error occurred", "INTERNAL_ERROR");
            } finally {
                MDC.remove("correlation_id");
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (StatusRuntimeException e) {
                // Pass through existing gRPC StatusRuntimeException with all metadata
                logError("gRPC status error in onReady", e);
                metrics.recordError(e.getStatus().getCode(), "STATUS_RUNTIME_EXCEPTION",
                        serverCall.getMethodDescriptor().getFullMethodName());
                metrics.stopTimer(timerSample, serverCall.getMethodDescriptor().getFullMethodName(),
                        e.getStatus().getCode());
                serverCall.close(e.getStatus(), e.getTrailers() != null ? e.getTrailers() : new Metadata());
            } catch (Exception e) {
                logError("Error in onReady", e);
                handleException(e, Status.INTERNAL, "An unexpected error occurred", "INTERNAL_ERROR");
            }
        }

        @Override
        public void onComplete() {
            try {
                super.onComplete();
                // Record successful completion
                metrics.stopTimer(timerSample, serverCall.getMethodDescriptor().getFullMethodName(), Status.Code.OK);
            } catch (Exception e) {
                logError("Error in onComplete", e);
            }
        }

        private void logError(String context, Exception e) {
            try (MDC.MDCCloseable ignored = MDC.putCloseable("correlation_id", correlationId)) {
                MDC.put("method", serverCall.getMethodDescriptor().getFullMethodName());
                MDC.put("service", serverCall.getMethodDescriptor().getServiceName());

                if (e instanceof StatusRuntimeException) {
                    StatusRuntimeException sre = (StatusRuntimeException) e;
                    MDC.put("status_code", sre.getStatus().getCode().name());
                }

                logger.error("{}: {}", context, e.getMessage(), e);
            } finally {
                MDC.remove("method");
                MDC.remove("service");
                MDC.remove("status_code");
            }
        }

        private void handleException(Exception e, Status status, String message, String errorCode) {
            // Record the error in metrics
            metrics.recordError(status.getCode(), errorCode,
                    serverCall.getMethodDescriptor().getFullMethodName());

            // Record timing
            metrics.stopTimer(timerSample, serverCall.getMethodDescriptor().getFullMethodName(),
                    status.getCode());

            Metadata trailers = new Metadata();

            // Add correlation ID for tracing
            trailers.put(CORRELATION_ID_KEY, correlationId);

            // Add error code for machine-readable categorization
            trailers.put(ERROR_CODE_KEY, errorCode);

            // Add detailed error message
            trailers.put(ERROR_DETAILS_KEY, e.getMessage());

            // Add cause if available
            if (e.getCause() != null) {
                trailers.put(ERROR_CAUSE_KEY, e.getCause().getMessage());
            }

            // Add location for debugging in non-production environments
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace.length > 0) {
                trailers.put(ERROR_LOCATION_KEY, stackTrace[0].toString());
            }

            // Close the call with appropriate status and trailers
            serverCall.close(status.withDescription(message), trailers);
        }

        /**
         * Format validation errors into a human-readable message
         * 
         * @param ex The validation exception containing field errors
         * @return A formatted error message
         */
        private String formatValidationErrors(ValidationException ex) {
            StringBuilder sb = new StringBuilder("Validation failed: ");
            boolean first = true;

            for (ValidationException.ValidationError error : ex.getValidationErrors()) {
                if (!first) {
                    sb.append("; ");
                }
                sb.append(error.getField()).append(": ").append(error.getMessage());
                first = false;
            }

            return sb.toString();
        }
    }

    /**
     * Generate a unique correlation ID for request tracing
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}