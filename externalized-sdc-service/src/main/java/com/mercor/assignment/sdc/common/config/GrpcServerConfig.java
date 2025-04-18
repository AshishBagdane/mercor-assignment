package com.mercor.assignment.sdc.common.config;

import com.mercor.assignment.sdc.grpc.service.JobGrpcServiceImpl;
import com.mercor.assignment.sdc.grpc.service.PaymentLineItemGrpcServiceImpl;
import com.mercor.assignment.sdc.grpc.service.SCDGrpcServiceImpl;
import com.mercor.assignment.sdc.grpc.service.TimelogGrpcServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for gRPC server with proper lifecycle management
 */
@Configuration
public class GrpcServerConfig {

  @Value("${grpc.server.port:9090}")
  private int grpcServerPort;

  private final SCDGrpcServiceImpl scdGrpcService;
  private final JobGrpcServiceImpl jobGrpcService;
  private final TimelogGrpcServiceImpl timelogGrpcService;
  private final PaymentLineItemGrpcServiceImpl paymentLineItemGrpcService;

  @Autowired
  public GrpcServerConfig(
      SCDGrpcServiceImpl scdGrpcService,
      JobGrpcServiceImpl jobGrpcService,
      TimelogGrpcServiceImpl timelogGrpcService,
      PaymentLineItemGrpcServiceImpl paymentLineItemGrpcService) {
    this.scdGrpcService = scdGrpcService;
    this.jobGrpcService = jobGrpcService;
    this.timelogGrpcService = timelogGrpcService;
    this.paymentLineItemGrpcService = paymentLineItemGrpcService;
  }

  /**
   * Create a lifecycle-managed gRPC server bean
   *
   * @return A SmartLifecycle-enabled gRPC server wrapper
   */
  @Bean
  public GrpcServerLifecycle grpcServerLifecycle() {
    return new GrpcServerLifecycle(grpcServerPort,
        scdGrpcService,
        jobGrpcService,
        timelogGrpcService,
        paymentLineItemGrpcService);
  }

  /**
   * A class that wraps the gRPC server and manages its lifecycle according to Spring's lifecycle management
   */
  public static class GrpcServerLifecycle implements SmartLifecycle {

    private final int port;
    private final SCDGrpcServiceImpl scdGrpcService;
    private final JobGrpcServiceImpl jobGrpcService;
    private final TimelogGrpcServiceImpl timelogGrpcService;
    private final PaymentLineItemGrpcServiceImpl paymentLineItemGrpcService;

    private Server server;
    private boolean running = false;

    public GrpcServerLifecycle(int port,
        SCDGrpcServiceImpl scdGrpcService,
        JobGrpcServiceImpl jobGrpcService,
        TimelogGrpcServiceImpl timelogGrpcService,
        PaymentLineItemGrpcServiceImpl paymentLineItemGrpcService) {
      this.port = port;
      this.scdGrpcService = scdGrpcService;
      this.jobGrpcService = jobGrpcService;
      this.timelogGrpcService = timelogGrpcService;
      this.paymentLineItemGrpcService = paymentLineItemGrpcService;
    }

    @Override
    public void start() {
      try {
        server = ServerBuilder.forPort(port)
            .addService(scdGrpcService)
            .addService(jobGrpcService)
            .addService(timelogGrpcService)
            .addService(paymentLineItemGrpcService)
            .build()
            .start();

        running = true;

        System.out.println("gRPC Server started, listening on port " + port);
      } catch (IOException e) {
        throw new RuntimeException("Failed to start gRPC server", e);
      }
    }

    @Override
    public void stop() {
      if (server != null) {
        try {
          server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
          System.out.println("gRPC server shut down successfully");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          System.err.println("gRPC server shutdown interrupted");
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