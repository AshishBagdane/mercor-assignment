package com.mercor.assignment.sdc.grpc.service;

import com.mercor.assignment.sdc.domain.dto.TimelogDTO;
import com.mercor.assignment.sdc.exception.EntityNotFoundException;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.service.TimelogService;
import com.mercor.scd.grpc.AdjustTimelogRequest;
import com.mercor.scd.grpc.GetTimelogsForContractorRequest;
import com.mercor.scd.grpc.GetTimelogsForJobRequest;
import com.mercor.scd.grpc.GetTimelogsWithDurationAboveRequest;
import com.mercor.scd.grpc.Timelog;
import com.mercor.scd.grpc.TimelogListResponse;
import com.mercor.scd.grpc.TimelogResponse;
import com.mercor.scd.grpc.TimelogServiceGrpc;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the gRPC TimelogService
 */
@Service
public class TimelogGrpcServiceImpl extends TimelogServiceGrpc.TimelogServiceImplBase {

  private static final Logger logger = LoggerFactory.getLogger(TimelogGrpcServiceImpl.class);
  private final TimelogService timelogService;

  @Autowired
  public TimelogGrpcServiceImpl(TimelogService timelogService) {
    this.timelogService = timelogService;
  }

  @Override
  public void getTimelogsForJob(GetTimelogsForJobRequest request,
      StreamObserver<TimelogListResponse> responseObserver) {
    try {
      List<TimelogDTO> timelogs = timelogService.getTimelogsForJob(request.getJobUid());

      TimelogListResponse response = TimelogListResponse.newBuilder()
          .addAllTimelogs(timelogs.stream().map(this::convertDtoToTimelog).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void getTimelogsForContractor(GetTimelogsForContractorRequest request,
      StreamObserver<TimelogListResponse> responseObserver) {
    try {
      List<TimelogDTO> timelogs = timelogService.getTimelogsForContractor(
          request.getContractorId(),
          request.getStartTime(),
          request.getEndTime());

      TimelogListResponse response = TimelogListResponse.newBuilder()
          .addAllTimelogs(timelogs.stream().map(this::convertDtoToTimelog).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void getTimelogsWithDurationAbove(GetTimelogsWithDurationAboveRequest request,
      StreamObserver<TimelogListResponse> responseObserver) {
    try {
      List<TimelogDTO> timelogs = timelogService.getTimelogsWithDurationAbove(request.getDuration());

      TimelogListResponse response = TimelogListResponse.newBuilder()
          .addAllTimelogs(timelogs.stream().map(this::convertDtoToTimelog).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void adjustTimelog(AdjustTimelogRequest request, StreamObserver<TimelogResponse> responseObserver) {
    try {
      if (request.getId() == null || request.getId().isEmpty()) {
        throw new SCDException("Timelog ID is required", "MISSING_TIMELOG_ID");
      }

      if (request.getAdjustedDuration() <= 0) {
        throw new SCDException("Adjusted duration must be greater than zero", "INVALID_DURATION");
      }

      TimelogDTO timelog = timelogService.adjustTimelog(request.getId(), request.getAdjustedDuration());

      TimelogResponse response = TimelogResponse.newBuilder()
          .setTimelog(convertDtoToTimelog(timelog))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      logger.warn("Timelog not found: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Timelog not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      logger.warn("Invalid argument in adjustTimelog: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          e.getMessage(),
          e));
    } catch (Exception e) {
      logger.error("Error in adjustTimelog call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during timelog adjustment: " + e.getMessage(),
          e));
    }
  }

  /**
   * Create a proper gRPC error response with detailed metadata
   *
   * @param status  The gRPC status code
   * @param message The error message
   * @param error   The original exception
   * @return StatusRuntimeException with detailed metadata
   */
  private StatusRuntimeException createDetailedError(Status status, String message, Throwable error) {
    Metadata metadata = new Metadata();
    Metadata.Key<String> errorDetailsKey = Metadata.Key.of("error-details", Metadata.ASCII_STRING_MARSHALLER);
    metadata.put(errorDetailsKey, error.getMessage());

    if (error instanceof SCDException) {
      metadata.put(
          Metadata.Key.of("error-code", Metadata.ASCII_STRING_MARSHALLER),
          ((SCDException) error).getErrorCode());
    }

    if (error.getCause() != null) {
      metadata.put(
          Metadata.Key.of("error-cause", Metadata.ASCII_STRING_MARSHALLER),
          error.getCause().getMessage());
    }

    return status
        .withDescription(message)
        .withCause(error)
        .asRuntimeException(metadata);
  }

  /**
   * Convert a TimelogDTO to a gRPC Timelog
   *
   * @param dto The TimelogDTO to convert
   * @return The gRPC Timelog
   */
  private Timelog convertDtoToTimelog(TimelogDTO dto) {
    return Timelog.newBuilder()
        .setId(dto.getId())
        .setVersion(dto.getVersion())
        .setUid(dto.getUid())
        .setCreatedAt(dto.getCreatedAt().getTime())
        .setUpdatedAt(dto.getUpdatedAt().getTime())
        .setDuration(dto.getDuration())
        .setTimeStart(dto.getTimeStart())
        .setTimeEnd(dto.getTimeEnd())
        .setType(dto.getType())
        .setJobUid(dto.getJobUid())
        .build();
  }

  /**
   * Convert a gRPC Timelog to a TimelogDTO
   *
   * @param timelog The gRPC Timelog to convert
   * @return The TimelogDTO
   */
  private TimelogDTO convertTimelogToDto(Timelog timelog) {
    return TimelogDTO.builder()
        .id(timelog.getId())
        .version(timelog.getVersion())
        .uid(timelog.getUid())
        .createdAt(new Date(timelog.getCreatedAt()))
        .updatedAt(new Date(timelog.getUpdatedAt()))
        .duration(timelog.getDuration())
        .timeStart(timelog.getTimeStart())
        .timeEnd(timelog.getTimeEnd())
        .type(timelog.getType())
        .jobUid(timelog.getJobUid())
        .build();
  }
}