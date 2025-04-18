package com.mercor.assignment.scd.service.grpc;

import com.mercor.assignment.scd.grpc.EntityRequest;
import com.mercor.assignment.scd.grpc.EntityResponse;
import com.mercor.assignment.scd.grpc.PingRequest;
import com.mercor.assignment.scd.grpc.PingResponse;
import com.mercor.assignment.scd.grpc.TestServiceGrpc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.Server;

import static org.assertj.core.api.Assertions.assertThat;

import com.mercor.assignment.scd.MercorScdApplication;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = MercorScdApplication.class)
public class TestServiceTest {

    private TestServiceGrpc.TestServiceBlockingStub testService;
    private Server server;
    private ManagedChannel channel;
    private final String serverName = "test-server";

    @BeforeEach
    public void setup() throws Exception {
        // Create a server, add service, and start
        server = InProcessServerBuilder
            .forName(serverName)
            .directExecutor()
            .addService(new TestServiceImpl())
            .build()
            .start();

        // Create a client channel
        channel = InProcessChannelBuilder
            .forName(serverName)
            .directExecutor()
            .build();

        // Create the stub
        testService = TestServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Shutdown the channel and server
        if (channel != null) {
            channel.shutdownNow();
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow();
            server.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void pingTest() {
        // Create a request
        PingRequest request = PingRequest.newBuilder()
            .setMessage("Hello from test")
            .build();

        // Call the service
        PingResponse response = testService.ping(request);

        // Verify the response
        assertThat(response.getMessage()).contains("Pong: Hello from test");
        assertThat(response.getTimestamp()).isGreaterThan(0);
    }

    @Test
    public void getLatestVersionTest() {
        // Create a request
        EntityRequest request = EntityRequest.newBuilder()
            .setEntityId("job_123")
            .setEntityType("job")
            .build();

        // Call the service
        EntityResponse response = testService.getLatestVersion(request);

        // Verify the response
        assertThat(response.getId()).isEqualTo("job_123");
        assertThat(response.getIsLatest()).isTrue();
        assertThat(response.getVersion()).isEqualTo(3);
    }
}