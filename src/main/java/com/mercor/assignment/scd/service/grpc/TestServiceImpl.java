package com.mercor.assignment.scd.service.grpc;

import com.mercor.assignment.scd.EntityRequest;
import com.mercor.assignment.scd.EntityResponse;
import com.mercor.assignment.scd.PingRequest;
import com.mercor.assignment.scd.PingResponse;
import com.mercor.assignment.scd.TestServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class TestServiceImpl extends TestServiceGrpc.TestServiceImplBase {

    @Override
    public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
        // Create a response with the current timestamp
        PingResponse response = PingResponse.newBuilder()
                .setMessage("Pong: " + request.getMessage())
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        // Send the response back
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getLatestVersion(EntityRequest request, StreamObserver<EntityResponse> responseObserver) {
        // This is a mock implementation - in a real scenario, you would query your database
        EntityResponse response = EntityResponse.newBuilder()
                .setUid("mock_uid_" + request.getEntityId())
                .setId(request.getEntityId())
                .setVersion(3) // Assuming version 3 is the latest
                .setIsLatest(true)
                .setCreatedAt(Instant.now().toEpochMilli())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}