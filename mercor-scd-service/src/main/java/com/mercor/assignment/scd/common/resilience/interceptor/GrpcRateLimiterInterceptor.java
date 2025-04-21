package com.mercor.assignment.scd.common.resilience.interceptor;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.grpc.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcRateLimiterInterceptor implements ServerInterceptor {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final Map<String, RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    public GrpcRateLimiterInterceptor(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
        log.info("Rate limiter interceptor initialized with registry: {}",
            rateLimiterRegistry.getAllRateLimiters().stream()
                .map(RateLimiter::getName)
                .toList());
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        Metadata headers,
        ServerCallHandler<ReqT, RespT> next) {

        String serviceName = extractServiceName(call.getMethodDescriptor().getServiceName());
        String methodName = call.getMethodDescriptor().getBareMethodName();

        log.info("RATE LIMITER ACTIVE: Checking call to {}.{}", serviceName, methodName);

        String rateLimiterName = mapServiceToRateLimiterName(serviceName);
        RateLimiter rateLimiter = getRateLimiter(rateLimiterName);

        boolean permitted = rateLimiter.acquirePermission();
        log.info("permission granted: {}", permitted);

        log.info("Rate limiter '{}' - permission granted: {}, available permits now: {}",
            rateLimiterName, permitted, rateLimiter.getMetrics().getAvailablePermissions());

        if (!permitted) {
            log.warn("DENIED: Rate limit exceeded for {}.{}", serviceName, methodName);
            call.close(Status.RESOURCE_EXHAUSTED
                    .withDescription("Rate limit exceeded for service: " + serviceName),
                new Metadata());
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }

    private String extractServiceName(String fullName) {
        int lastDotIndex = fullName.lastIndexOf('.');
        return lastDotIndex >= 0 ? fullName.substring(lastDotIndex + 1) : fullName;
    }

    private String mapServiceToRateLimiterName(String serviceName) {
        if ("ServerReflection".equals(serviceName)) {
            return "serverReflection";
        }
        return switch (serviceName) {
            case "JobService" -> "jobService";
            case "SCDService" -> "scdService";
            case "PaymentLineItemService" -> "paymentLineItemService";
            case "TimelogService" -> "timelogService";
            default -> "default";
        };
    }

    private RateLimiter getRateLimiter(String name) {
        return rateLimiterCache.computeIfAbsent(name, rateLimiterRegistry::rateLimiter);
    }
}
