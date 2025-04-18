package com.mercor.assignment.sdc.common.config;

import com.mercor.scd.grpc.JobServiceGrpc;
import com.mercor.scd.grpc.PaymentLineItemServiceGrpc;
import com.mercor.scd.grpc.SCDServiceGrpc;
import com.mercor.scd.grpc.TimelogServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for gRPC client stubs This can be used by other services to connect to the SCD service
 */
@Configuration
public class GrpcClientConfig {

  @Value("${grpc.client.scd-service.host:localhost}")
  private String scdServiceHost;

  @Value("${grpc.client.scd-service.port:9090}")
  private int scdServicePort;

  private ManagedChannel channel;

  /**
   * Create a gRPC channel to the SCD service
   *
   * @return The gRPC channel
   */
  @Bean
  public ManagedChannel scdServiceChannel() {
    channel = ManagedChannelBuilder.forAddress(scdServiceHost, scdServicePort)
        .usePlaintext() // Disable TLS for simplicity (not recommended for production)
        .build();
    return channel;
  }

  /**
   * Create a stub for the SCDService
   *
   * @param channel The gRPC channel
   * @return The SCDService stub
   */
  @Bean
  public SCDServiceGrpc.SCDServiceBlockingStub scdServiceStub(ManagedChannel channel) {
    return SCDServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Create a stub for the JobService
   *
   * @param channel The gRPC channel
   * @return The JobService stub
   */
  @Bean
  public JobServiceGrpc.JobServiceBlockingStub jobServiceStub(ManagedChannel channel) {
    return JobServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Create a stub for the TimelogService
   *
   * @param channel The gRPC channel
   * @return The TimelogService stub
   */
  @Bean
  public TimelogServiceGrpc.TimelogServiceBlockingStub timelogServiceStub(ManagedChannel channel) {
    return TimelogServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Create a stub for the PaymentLineItemService
   *
   * @param channel The gRPC channel
   * @return The PaymentLineItemService stub
   */
  @Bean
  public PaymentLineItemServiceGrpc.PaymentLineItemServiceBlockingStub paymentLineItemServiceStub(ManagedChannel channel) {
    return PaymentLineItemServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Shutdown the gRPC channel when the application is shutting down
   *
   * @throws InterruptedException If the shutdown is interrupted
   */
  @PreDestroy
  public void closeChannel() throws InterruptedException {
    if (channel != null) {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}