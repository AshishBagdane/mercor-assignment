package com.mercor.assignment.scd.domain.job.mapper;

import com.mercor.assignment.scd.domain.job.CreateNewJobRequest;
import com.mercor.assignment.scd.domain.job.JobProto;
import com.mercor.assignment.scd.domain.job.JobResponse;
import com.mercor.assignment.scd.domain.job.JobListResponse;
import com.mercor.assignment.scd.domain.job.GetActiveJobsForCompanyRequest;
import com.mercor.assignment.scd.domain.job.GetActiveJobsForContractorRequest;
import com.mercor.assignment.scd.domain.job.GetJobsWithRateAboveRequest;
import com.mercor.assignment.scd.domain.job.UpdateJobStatusRequest;
import com.mercor.assignment.scd.domain.job.UpdateJobRateRequest;
import com.mercor.assignment.scd.domain.job.model.Job;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between Job proto messages and Job entity
 */
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobMapper {

    /**
     * Maps a JobProto message to a Job entity
     *
     * @param jobProto the JobProto message
     * @return the Job entity
     */
    @Mapping(source = "id", target = "id")
    @Mapping(source = "version", target = "version")
    @Mapping(source = "uid", target = "uid")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "timestampToInstant")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "timestampToInstant")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "rate", target = "rate", qualifiedByName = "doubleToDecimal")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "companyId", target = "companyId")
    @Mapping(source = "contractorId", target = "contractorId")
    Job toEntity(JobProto jobProto);

    /**
     * Maps a JobResponse to a Job entity
     *
     * @param response the JobResponse
     * @return the Job entity
     */
    default Job toEntity(JobResponse response) {
        if (response == null || response.getJob() == null) {
            return null;
        }
        return toEntity(response.getJob());
    }

    /**
     * Maps a JobListResponse to a list of Job entities
     *
     * @param response the JobListResponse
     * @return the list of Job entities
     */
    default List<Job> toEntityList(JobListResponse response) {
        if (response == null) {
            return null;
        }
        return response.getJobsList().stream()
            .map(this::toEntity)
            .toList();
    }

    /**
     * Maps a CreateNewJobRequest to a Job entity
     *
     * @param request the CreateNewJobRequest
     * @return the Job entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "status", target = "status")
    @Mapping(source = "rate", target = "rate", qualifiedByName = "doubleToDecimal")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "companyId", target = "companyId")
    @Mapping(source = "contractorId", target = "contractorId")
    Job toEntity(CreateNewJobRequest request);

    /**
     * Maps a Job entity to a JobProto message
     *
     * @param job the Job entity
     * @return the JobProto message
     */
    @Mapping(source = "rate", target = "rate", qualifiedByName = "decimalToDouble")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "instantToTimestamp")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "instantToTimestamp")
    JobProto toProto(Job job);

    /**
     * Maps a Job entity to a JobResponse
     *
     * @param job the Job entity
     * @return the JobResponse
     */
    default JobResponse toResponse(Job job) {
        if (job == null) {
            return null;
        }
        return JobResponse.newBuilder()
            .setJob(toProto(job))
            .build();
    }

    /**
     * Maps a list of Job entities to a JobListResponse
     *
     * @param jobs the list of Job entities
     * @return the JobListResponse
     */
    default JobListResponse toListResponse(List<Job> jobs) {
        if (jobs == null) {
            return null;
        }
        return JobListResponse.newBuilder()
            .addAllJobs(jobs.stream().map(this::toProto).toList())
            .build();
    }

    /**
     * Updates a Job entity based on UpdateJobStatusRequest
     *
     * @param request the UpdateJobStatusRequest
     * @param job the Job entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "rate", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "contractorId", ignore = true)
    @Mapping(source = "status", target = "status")
    void updateStatusFromRequest(UpdateJobStatusRequest request, @MappingTarget Job job);

    /**
     * Updates a Job entity based on UpdateJobRateRequest
     *
     * @param request the UpdateJobRateRequest
     * @param job the Job entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "companyId", ignore = true)
    @Mapping(target = "contractorId", ignore = true)
    @Mapping(source = "rate", target = "rate", qualifiedByName = "doubleToDecimal")
    void updateRateFromRequest(UpdateJobRateRequest request, @MappingTarget Job job);

    /**
     * Creates a GetActiveJobsForCompanyRequest from a company ID
     *
     * @param companyId the company ID
     * @return the GetActiveJobsForCompanyRequest
     */
    default GetActiveJobsForCompanyRequest toCompanyRequest(String companyId) {
        return GetActiveJobsForCompanyRequest.newBuilder()
            .setCompanyId(companyId)
            .build();
    }

    /**
     * Creates a GetActiveJobsForContractorRequest from a contractor ID
     *
     * @param contractorId the contractor ID
     * @return the GetActiveJobsForContractorRequest
     */
    default GetActiveJobsForContractorRequest toContractorRequest(String contractorId) {
        return GetActiveJobsForContractorRequest.newBuilder()
            .setContractorId(contractorId)
            .build();
    }

    /**
     * Creates a GetJobsWithRateAboveRequest from a rate value
     *
     * @param rate the rate value
     * @return the GetJobsWithRateAboveRequest
     */
    default GetJobsWithRateAboveRequest toRateRequest(BigDecimal rate) {
        return GetJobsWithRateAboveRequest.newBuilder()
            .setRate(rate.doubleValue())
            .build();
    }

    /**
     * Creates an UpdateJobStatusRequest from a job ID and status
     *
     * @param jobId the job ID
     * @param status the status
     * @return the UpdateJobStatusRequest
     */
    default UpdateJobStatusRequest toStatusUpdateRequest(String jobId, String status) {
        return UpdateJobStatusRequest.newBuilder()
            .setId(jobId)
            .setStatus(status)
            .build();
    }

    /**
     * Creates an UpdateJobRateRequest from a job ID and rate
     *
     * @param jobId the job ID
     * @param rate the rate
     * @return the UpdateJobRateRequest
     */
    default UpdateJobRateRequest toRateUpdateRequest(String jobId, BigDecimal rate) {
        return UpdateJobRateRequest.newBuilder()
            .setId(jobId)
            .setRate(rate.doubleValue())
            .build();
    }

    /**
     * Converts a proto timestamp (milliseconds since epoch) to Java's Instant
     *
     * @param timestamp the timestamp in milliseconds
     * @return the Instant
     */
    @Named("timestampToInstant")
    default Instant timestampToInstant(long timestamp) {
        if (timestamp == 0) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp);
    }

    /**
     * Converts a Java Instant to proto timestamp (milliseconds since epoch)
     *
     * @param instant the Instant
     * @return the timestamp in milliseconds
     */
    @Named("instantToTimestamp")
    default long instantToTimestamp(Instant instant) {
        if (instant == null) {
            return 0;
        }
        return instant.toEpochMilli();
    }

    /**
     * Converts a proto double to Java's BigDecimal
     *
     * @param value the double value
     * @return the BigDecimal
     */
    @Named("doubleToDecimal")
    default BigDecimal doubleToDecimal(double value) {
        return BigDecimal.valueOf(value);
    }

    /**
     * Converts a Java BigDecimal to proto double
     *
     * @param value the BigDecimal
     * @return the double value
     */
    @Named("decimalToDouble")
    default double decimalToDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }
        return value.doubleValue();
    }
}
