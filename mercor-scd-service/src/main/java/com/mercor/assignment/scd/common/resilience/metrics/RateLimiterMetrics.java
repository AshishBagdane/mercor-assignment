package com.mercor.assignment.scd.common.resilience.metrics;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RateLimiterMetrics {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        // Register custom metrics for each rate limiter
        rateLimiterRegistry.getAllRateLimiters().forEach(rateLimiter -> {
            String name = rateLimiter.getName();
            Tags tags = Tags.of("name", name);

            // Register gauge for available permissions
            meterRegistry.gauge(
                "resilience4j.ratelimiter.available.permissions",
                tags,
                rateLimiter,
                rl -> rl.getMetrics().getAvailablePermissions()
            );

            // Register gauge for waiting threads
            meterRegistry.gauge(
                "resilience4j.ratelimiter.waiting.threads",
                tags,
                rateLimiter,
                rl -> rl.getMetrics().getNumberOfWaitingThreads()
            );
        });
    }
}