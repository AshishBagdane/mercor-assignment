package com.mercor.assignment.scd.domain.timelog.service.grpc;

import com.mercor.assignment.scd.domain.timelog.AdjustTimelogRequest;
import com.mercor.assignment.scd.domain.timelog.CreateNewTimelogForJobRequest;
import com.mercor.assignment.scd.domain.timelog.GetTimelogsForContractorRequest;
import com.mercor.assignment.scd.domain.timelog.GetTimelogsForJobRequest;
import com.mercor.assignment.scd.domain.timelog.GetTimelogsWithDurationAboveRequest;
import com.mercor.assignment.scd.domain.timelog.TimelogListResponse;
import com.mercor.assignment.scd.domain.timelog.TimelogResponse;
import com.mercor.assignment.scd.domain.timelog.TimelogServiceGrpc;
import com.mercor.assignment.scd.domain.timelog.mapper.TimelogMapper;
import com.mercor.assignment.scd.domain.timelog.model.Timelog;
import com.mercor.assignment.scd.domain.timelog.service.regular.TimelogService;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TimelogGrpcServiceImpl extends TimelogServiceGrpc.TimelogServiceImplBase {

  private final TimelogService timelogService;
  private final TimelogMapper timelogMapper;

    @Override
    public void createNewTimelogForJob(final CreateNewTimelogForJobRequest request,
                                       final StreamObserver<TimelogResponse> responseObserver) {
        final Timelog timelog = timelogMapper.toEntity(request);

        final TimelogResponse response = timelogMapper.toTimelogResponse(timelogService.createEntity(timelog));

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
  public void getTimelogsForJob(GetTimelogsForJobRequest request, StreamObserver<TimelogListResponse> responseObserver) {
    final List<Timelog> timelogs = timelogService.findTimelogsForJob(request.getJobUid());

    final TimelogListResponse response = timelogMapper.toTimelogListResponse(timelogs);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getTimelogsForContractor(GetTimelogsForContractorRequest request, StreamObserver<TimelogListResponse> responseObserver) {
    final List<Timelog> timelog = timelogService.findTimelogsForContractor(request.getContractorId(), request.getStartTime(), request.getEndTime());

    final TimelogListResponse response = timelogMapper.toTimelogListResponse(timelog);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getTimelogsWithDurationAbove(GetTimelogsWithDurationAboveRequest request, StreamObserver<TimelogListResponse> responseObserver) {
    final List<Timelog> timelog = timelogService.findTimelogsWithDurationAbove(request.getDuration());

    final TimelogListResponse response = timelogMapper.toTimelogListResponse(timelog);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void adjustTimelog(AdjustTimelogRequest request, StreamObserver<TimelogResponse> responseObserver) {
    final Timelog newTimelogVersion = timelogService.adjustTimelog(request.getId(), request.getAdjustedDuration());
    final TimelogResponse response = timelogMapper.toTimelogResponse(newTimelogVersion);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
