package com.mercor.assignment.sdc.grpc.service;

import com.mercor.assignment.sdc.domain.dto.JobDTO;
import com.mercor.assignment.sdc.exception.EntityNotFoundException;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.service.JobService;
import com.mercor.scd.grpc.GetActiveJobsForCompanyRequest;
import com.mercor.scd.grpc.GetActiveJobsForContractorRequest;
import com.mercor.scd.grpc.GetJobsWithRateAboveRequest;
import com.mercor.scd.grpc.Job;
import com.mercor.scd.grpc.JobListResponse;
import com.mercor.scd.grpc.JobResponse;
import com.mercor.scd.grpc.JobServiceGrpc;
import com.mercor.scd.grpc.UpdateJobRateRequest;
import com.mercor.scd.grpc.UpdateJobStatusRequest;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the gRPC JobService
 */
@Service
public class JobGrpcServiceImpl extends JobServiceGrpc.JobServiceImplBase {

  private static final Logger logger = LoggerFactory.getLogger(JobGrpcServiceImpl.class);
  private final JobService jobService;

  @Autowired
  public JobGrpcServiceImpl(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public void getActiveJobsForCompany(GetActiveJobsForCompanyRequest request,
      StreamObserver<JobListResponse> responseObserver) {
    try {
      if (request.getCompanyId() == null || request.getCompanyId().isEmpty()) {
        throw new SCDException("Company ID is required", "MISSING_COMPANY_ID");
      }

      List<JobDTO> jobs = jobService.getActiveJobsForCompany(request.getCompanyId());

      JobListResponse response = JobListResponse.newBuilder()
          .addAllJobs(jobs.stream().map(this::convertDtoToJob).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      logger.warn("Company not found: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Company not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      logger.warn("Invalid argument in getActiveJobsForCompany: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          e.getMessage(),
          e));
    } catch (Exception e) {
      logger.error("Error in getActiveJobsForCompany call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error: " + e.getMessage(),
          e));
    }
  }

  @Override
  public void getActiveJobsForContractor(GetActiveJobsForContractorRequest request,
      StreamObserver<JobListResponse> responseObserver) {
    try {
      if (request.getContractorId() == null || request.getContractorId().isEmpty()) {
        throw new SCDException("Contractor ID is required", "MISSING_CONTRACTOR_ID");
      }

      List<JobDTO> jobs = jobService.getActiveJobsForContractor(request.getContractorId());

      JobListResponse response = JobListResponse.newBuilder()
          .addAllJobs(jobs.stream().map(this::convertDtoToJob).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      logger.warn("Contractor not found: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Contractor not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      logger.warn("Invalid argument in getActiveJobsForContractor: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          e.getMessage(),
          e));
    } catch (Exception e) {
      logger.error("Error in getActiveJobsForContractor call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error: " + e.getMessage(),
          e));
    }
  }

  @Override
  public void getJobsWithRateAbove(GetJobsWithRateAboveRequest request,
      StreamObserver<JobListResponse> responseObserver) {
    try {
      if (request.getRate() < 0) {
        throw new SCDException("Rate cannot be negative", "INVALID_RATE");
      }

      List<JobDTO> jobs = jobService.getJobsWithRateAbove(request.getRate());

      JobListResponse response = JobListResponse.newBuilder()
          .addAllJobs(jobs.stream().map(this::convertDtoToJob).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (SCDException e) {
      logger.warn("Invalid argument in getJobsWithRateAbove: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          e.getMessage(),
          e));
    } catch (Exception e) {
      logger.error("Error in getJobsWithRateAbove call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error: " + e.getMessage(),
          e));
    }
  }

  @Override
  public void updateStatus(UpdateJobStatusRequest request, StreamObserver<JobResponse> responseObserver) {
    try {
      if (request.getId() == null || request.getId().isEmpty()) {
        throw new SCDException("Job ID is required", "MISSING_JOB_ID");
      }

      if (request.getStatus() == null || request.getStatus().isEmpty()) {
        throw new SCDException("Status is required", "MISSING_STATUS");
      }

      JobDTO job = jobService.updateStatus(request.getId(), request.getStatus());

      JobResponse response = JobResponse.newBuilder()
          .setJob(convertDtoToJob(job))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      logger.warn("Job not found: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Job not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      logger.warn("Invalid argument in updateStatus: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          e.getMessage(),
          e));
    } catch (Exception e) {
      logger.error("Error in updateStatus call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error: " + e.getMessage(),
          e));
    }
  }

  @Override
  public void updateRate(UpdateJobRateRequest request, StreamObserver<JobResponse> responseObserver) {
    try {
      if (request.getId() == null || request.getId().isEmpty()) {
        throw new SCDException("Job ID is required", "MISSING_JOB_ID");
      }

      if (request.getRate() <= 0) {
        throw new SCDException("Rate must be greater than zero", "INVALID_RATE");
      }

      JobDTO job = jobService.updateRate(request.getId(), request.getRate());

      JobResponse response = JobResponse.newBuilder()
          .setJob(convertDtoToJob(job))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      logger.warn("Job not found: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Job not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      logger.warn("Invalid argument in updateRate: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          e.getMessage(),
          e));
    } catch (Exception e) {
      logger.error("Error in updateRate call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during rate update: " + e.getMessage(),
          e));
    }
  }

  /**
   * Convert a JobDTO to a gRPC Job
   *
   * @param dto The JobDTO to convert
   * @return The gRPC Job
   */
  private Job convertDtoToJob(JobDTO dto) {
    return Job.newBuilder()
        .setId(dto.getId())
        .setVersion(dto.getVersion())
        .setUid(dto.getUid())
        .setCreatedAt(dto.getCreatedAt().getTime())
        .setUpdatedAt(dto.getUpdatedAt().getTime())
        .setStatus(dto.getStatus())
        .setRate(dto.getRate().doubleValue())
        .setTitle(dto.getTitle())
        .setCompanyId(dto.getCompanyId())
        .setContractorId(dto.getContractorId())
        .build();
  }

  /**
   * Convert a gRPC Job to a JobDTO
   *
   * @param job The gRPC Job to convert
   * @return The JobDTO
   */
  private JobDTO convertJobToDto(Job job) {
    return JobDTO.builder()
        .id(job.getId())
        .version(job.getVersion())
        .uid(job.getUid())
        .createdAt(new Date(job.getCreatedAt()))
        .updatedAt(new Date(job.getUpdatedAt()))
        .status(job.getStatus())
        .rate(BigDecimal.valueOf(job.getRate()))
        .title(job.getTitle())
        .companyId(job.getCompanyId())
        .contractorId(job.getContractorId())
        .build();
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
}