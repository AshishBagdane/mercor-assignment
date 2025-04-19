package com.mercor.assignment.scd.domain.job.service.grpc;

import com.mercor.assignment.scd.domain.job.GetActiveJobsForCompanyRequest;
import com.mercor.assignment.scd.domain.job.GetActiveJobsForContractorRequest;
import com.mercor.assignment.scd.domain.job.GetJobsWithRateAboveRequest;
import com.mercor.assignment.scd.domain.job.JobListResponse;
import com.mercor.assignment.scd.domain.job.JobResponse;
import com.mercor.assignment.scd.domain.job.JobServiceGrpc;
import com.mercor.assignment.scd.domain.job.UpdateJobRateRequest;
import com.mercor.assignment.scd.domain.job.UpdateJobStatusRequest;
import com.mercor.assignment.scd.domain.job.mapper.JobMapper;
import com.mercor.assignment.scd.domain.job.model.Job;
import com.mercor.assignment.scd.domain.job.service.JobService;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class JobGrpcServiceImpl extends JobServiceGrpc.JobServiceImplBase {

    private final JobService jobService;
    private final JobMapper jobMapper;

    @Override
    public void getActiveJobsForCompany(GetActiveJobsForCompanyRequest request, StreamObserver<JobListResponse> responseObserver) {
        final String companyId = request.getCompanyId();
        final List<Job> jobs = jobService.findActiveJobsForCompany(companyId);

        final JobListResponse response = jobMapper.toListResponse(jobs);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getActiveJobsForContractor(GetActiveJobsForContractorRequest request, StreamObserver<JobListResponse> responseObserver) {
        final String contractorId = request.getContractorId();
        final List<Job> jobs = jobService.findActiveJobsForContractor(contractorId);

        final JobListResponse response = jobMapper.toListResponse(jobs);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getJobsWithRateAbove(GetJobsWithRateAboveRequest request, StreamObserver<JobListResponse> responseObserver) {
        final Double rate = request.getRate();
        final List<Job> jobs = jobService.findJobsWithRateAbove(rate);

        final JobListResponse response = jobMapper.toListResponse(jobs);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateStatus(UpdateJobStatusRequest request, StreamObserver<JobResponse> responseObserver) {
        final Map<String, Object> map = new HashMap<>();
        map.put("status", request.getStatus());

        final Job newJobVersion = jobService.createNewVersion(request.getId(), map);
        final JobResponse response = jobMapper.toResponse(newJobVersion);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateRate(UpdateJobRateRequest request, StreamObserver<JobResponse> responseObserver) {
        final Map<String, Object> map = new HashMap<>();
        map.put("rate", request.getRate());

        final Job newJobVersion = jobService.createNewVersion(request.getId(), map);
        final JobResponse response = jobMapper.toResponse(newJobVersion);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}