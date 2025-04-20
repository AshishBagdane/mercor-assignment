package com.mercor.assignment.scd.common.config;

import com.mercor.assignment.scd.domain.TestServiceImpl;
import com.mercor.assignment.scd.domain.core.service.grpc.SCDGrpcServiceImpl;
import com.mercor.assignment.scd.domain.job.service.grpc.JobGrpcServiceImpl;
import com.mercor.assignment.scd.domain.paymentlineitem.service.grpc.PaymentLineItemGrpcServiceImpl;
import com.mercor.assignment.scd.domain.timelog.service.grpc.TimelogGrpcServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for gRPC server with proper lifecycle management
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GrpcServerConfig {

  @Value("${grpc.server.port:50051}")
  private int grpcServerPort;

  private final TestServiceImpl testService;
  private final SCDGrpcServiceImpl scdGrpcService;
  private final JobGrpcServiceImpl jobGrpcService;
  private final TimelogGrpcServiceImpl timelogGrpcService;
  private final PaymentLineItemGrpcServiceImpl paymentLineItemGrpcService;

  /**
   * Create a lifecycle-managed gRPC server bean
   *
   * @return A SmartLifecycle-enabled gRPC server wrapper
   */
  @Bean
  public GrpcServerLifecycle grpcServerLifecycle() {
    return new GrpcServerLifecycle(grpcServerPort,
        testService, scdGrpcService, jobGrpcService, timelogGrpcService, paymentLineItemGrpcService);
  }

  /**
   * A class that wraps the gRPC server and manages its lifecycle according to Spring's lifecycle management
   */
  public static class GrpcServerLifecycle implements SmartLifecycle {

    private final int port;
    private final TestServiceImpl testService;
    private final SCDGrpcServiceImpl scdGrpcService;
    private final JobGrpcServiceImpl jobGrpcService;
    private final TimelogGrpcServiceImpl timelogGrpcService;
    private final PaymentLineItemGrpcServiceImpl paymentLineItemGrpcService;

    private Server server;
    private boolean running = false;

    public GrpcServerLifecycle(int port,
        TestServiceImpl testService, SCDGrpcServiceImpl scdGrpcService, JobGrpcServiceImpl jobGrpcService, TimelogGrpcServiceImpl timelogGrpcService
    , PaymentLineItemGrpcServiceImpl paymentLineItemGrpcService) {
      this.port = port;
      this.testService = testService;
      this.scdGrpcService = scdGrpcService;
      this.jobGrpcService = jobGrpcService;
      this.timelogGrpcService = timelogGrpcService;
      this.paymentLineItemGrpcService = paymentLineItemGrpcService;
    }

    @Override
    public void start() {
      try {
        server = ServerBuilder.forPort(port)
            .addService(testService)
            .addService(scdGrpcService)
            .addService(jobGrpcService)
            .addService(timelogGrpcService)
            .addService(paymentLineItemGrpcService)
            .addService(ProtoReflectionServiceV1.newInstance())
            .build()
            .start();

        running = true;

        log.info("gRPC Server started, listening on port {}", port);
      } catch (IOException e) {
        throw new RuntimeException("Failed to start gRPC server", e);
      }
    }

    @Override
    public void stop() {
      if (server != null) {
        try {
          server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
          log.info("gRPC server shut down successfully");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.error("gRPC server shutdown interrupted");
        } finally {
          server = null;
          running = false;
        }
      }
    }

    @Override
    public boolean isRunning() {
      return running;
    }

    @Override
    public int getPhase() {
      // Start fairly early and shut down fairly late
      return 1000;
    }

    @Override
    public boolean isAutoStartup() {
      return true;
    }

    @Override
    public void stop(Runnable callback) {
      stop();
      callback.run();
    }
  }
}