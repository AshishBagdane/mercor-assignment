package com.mercor.assignment.scd.domain.paymentlineitem.service.grpc;

import com.mercor.assignment.scd.domain.paymentlineitem.mapper.PaymentLineItemMapper;
import com.mercor.assignment.scd.domain.paymentlineitem.model.PaymentLineItem;
import com.mercor.assignment.scd.domain.paymentlineitem.service.regular.PaymentLineItemService;
import com.mercor.assignment.scd.domain.paymentlineitems.GetPaymentLineItemsForContractorRequest;
import com.mercor.assignment.scd.domain.paymentlineitems.GetPaymentLineItemsForJobRequest;
import com.mercor.assignment.scd.domain.paymentlineitems.GetPaymentLineItemsForTimelogRequest;
import com.mercor.assignment.scd.domain.paymentlineitems.GetTotalAmountForContractorRequest;
import com.mercor.assignment.scd.domain.paymentlineitems.MarkAsPaidRequest;
import com.mercor.assignment.scd.domain.paymentlineitems.PaymentLineItemListResponse;
import com.mercor.assignment.scd.domain.paymentlineitems.PaymentLineItemResponse;
import com.mercor.assignment.scd.domain.paymentlineitems.PaymentLineItemServiceGrpc;
import com.mercor.assignment.scd.domain.paymentlineitems.TotalAmountResponse;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class PaymentLineItemGrpcServiceImpl extends PaymentLineItemServiceGrpc.PaymentLineItemServiceImplBase {

  private final PaymentLineItemService paymentLineItemService;
  private final PaymentLineItemMapper paymentLineItemMapper;

  @Override
  public void getPaymentLineItemsForJob(GetPaymentLineItemsForJobRequest request, StreamObserver<PaymentLineItemListResponse> responseObserver) {
    final List<PaymentLineItem> paymentLineItems = paymentLineItemService.getPaymentLineItemsForJob(request.getJobUid());

    final PaymentLineItemListResponse response = paymentLineItemMapper.toListResponse(paymentLineItems);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getPaymentLineItemsForTimelog(GetPaymentLineItemsForTimelogRequest request, StreamObserver<PaymentLineItemListResponse> responseObserver) {
    final List<PaymentLineItem> paymentLineItems = paymentLineItemService.getPaymentLineItemsForTimelog(request.getTimelogUid());

    final PaymentLineItemListResponse response = paymentLineItemMapper.toListResponse(paymentLineItems);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getPaymentLineItemsForContractor(GetPaymentLineItemsForContractorRequest request, StreamObserver<PaymentLineItemListResponse> responseObserver) {
    final List<PaymentLineItem> paymentLineItems = paymentLineItemService.getPaymentLineItemsForContractor(request.getContractorId(), request.getStartTime(), request.getEndTime());

    final PaymentLineItemListResponse response = paymentLineItemMapper.toListResponse(paymentLineItems);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void markAsPaid(MarkAsPaidRequest request, StreamObserver<PaymentLineItemResponse> responseObserver) {
    final PaymentLineItem paymentLineItem = paymentLineItemService.markAsPaid(request.getId());

    final PaymentLineItemResponse response = paymentLineItemMapper.toResponse(paymentLineItem);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getTotalAmountForContractor(GetTotalAmountForContractorRequest request, StreamObserver<TotalAmountResponse> responseObserver) {
    final BigDecimal totalAmountForContractor = paymentLineItemService.getTotalAmountForContractor(request.getContractorId(), request.getStartTime(), request.getEndTime());

    final TotalAmountResponse totalAmountResponse = TotalAmountResponse.newBuilder()
        .setTotalAmount(totalAmountForContractor.doubleValue())
        .build();

    responseObserver.onNext(totalAmountResponse);
    responseObserver.onCompleted();
  }
}
