package com.mercor.assignment.sdc.common.config;

import com.mercor.assignment.sdc.exception.GrpcExceptionInterceptor;
import com.mercor.assignment.sdc.metrics.GrpcErrorMetrics;
import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for gRPC server.
 * Registers interceptors for gRPC exception handling.
 */
@Configuration
public class GrpcConfig {

    /**
     * Register gRPC exception interceptor
     */
    @Bean
    public ServerInterceptor grpcExceptionInterceptor(GrpcErrorMetrics grpcErrorMetrics) {
        return new GrpcExceptionInterceptor(grpcErrorMetrics);
    }
}