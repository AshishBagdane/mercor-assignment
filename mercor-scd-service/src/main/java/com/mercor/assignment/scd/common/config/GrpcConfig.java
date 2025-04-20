package com.mercor.assignment.scd.common.config;

import com.mercor.assignment.scd.common.errorhandling.interceptor.GrpcExceptionInterceptor;
import com.mercor.assignment.scd.common.errorhandling.metrics.GrpcErrorMetrics;
import io.grpc.ServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

  @Bean
  public ServerInterceptor grpcExceptionInterceptor(GrpcErrorMetrics grpcErrorMetrics) {
    return new GrpcExceptionInterceptor(grpcErrorMetrics);
  }
}