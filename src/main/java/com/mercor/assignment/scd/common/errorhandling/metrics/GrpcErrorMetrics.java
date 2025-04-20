package com.mercor.assignment.scd.common.errorhandling.metrics;

import io.grpc.Status;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Metrics collector for gRPC requests and errors.
 * Provides counters and timers for monitoring gRPC service performance and
 * error rates.
 */
@Component
public class GrpcErrorMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Timer> methodTimers = new ConcurrentHashMap<>();

    public GrpcErrorMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record a gRPC error by status code and error code
     * 
     * @param statusCode The gRPC status code of the error
     * @param errorCode  The application-specific error code
     * @param method     The gRPC method that generated the error
     */
    public void recordError(Status.Code statusCode, String errorCode, String method) {
        Counter.builder("grpc.server.errors")
                .tag("status", statusCode.name())
                .tag("error_code", errorCode != null ? errorCode : "UNKNOWN")
                .tag("method", method)
                .description("Count of gRPC errors by status code and error code")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Start timing a gRPC method call
     * 
     * @param fullMethodName The full name of the gRPC method being called
     * @return A timer sample that can be used to stop the timer
     */
    public Timer.Sample startTimer(String fullMethodName) {
        return Timer.start(meterRegistry);
    }

    /**
     * Stop timing a gRPC method call and record the duration
     * 
     * @param sample         The timer sample from startTimer
     * @param fullMethodName The full name of the gRPC method being called
     * @param statusCode     The status code of the completed call
     */
    public void stopTimer(Timer.Sample sample, String fullMethodName, Status.Code statusCode) {
        String timerKey = fullMethodName + "." + statusCode.name();
        Timer timer = methodTimers.computeIfAbsent(timerKey, key -> Timer.builder("grpc.server.duration")
                .tag("method", fullMethodName)
                .tag("status", statusCode.name())
                .description("Duration of gRPC method calls")
                .register(meterRegistry));

        sample.stop(timer);
    }

    /**
     * Record the total size of a gRPC request and response
     * 
     * @param fullMethodName The full name of the gRPC method
     * @param requestSize    The size of the request in bytes
     * @param responseSize   The size of the response in bytes
     */
    public void recordMessageSizes(String fullMethodName, long requestSize, long responseSize) {
        // Record request size
        meterRegistry.summary("grpc.server.request.size", "method", fullMethodName)
                .record(requestSize);

        // Record response size
        meterRegistry.summary("grpc.server.response.size", "method", fullMethodName)
                .record(responseSize);
    }
}