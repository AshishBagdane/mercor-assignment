package com.mercor.assignment.sdc.grpc.service;

import com.mercor.assignment.sdc.domain.dto.PaymentLineItemDTO;
import com.mercor.assignment.sdc.exception.EntityNotFoundException;
import com.mercor.assignment.sdc.exception.SCDException;
import com.mercor.assignment.sdc.service.PaymentLineItemService;
import com.mercor.scd.grpc.GetPaymentLineItemsForContractorRequest;
import com.mercor.scd.grpc.GetPaymentLineItemsForJobRequest;
import com.mercor.scd.grpc.GetPaymentLineItemsForTimelogRequest;
import com.mercor.scd.grpc.GetTotalAmountForContractorRequest;
import com.mercor.scd.grpc.MarkAsPaidRequest;
import com.mercor.scd.grpc.PaymentLineItem;
import com.mercor.scd.grpc.PaymentLineItemListResponse;
import com.mercor.scd.grpc.PaymentLineItemResponse;
import com.mercor.scd.grpc.PaymentLineItemServiceGrpc;
import com.mercor.scd.grpc.TotalAmountResponse;
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
 * Implementation of the gRPC PaymentLineItemService
 */
@Service
public class PaymentLineItemGrpcServiceImpl extends PaymentLineItemServiceGrpc.PaymentLineItemServiceImplBase {

  private static final Logger logger = LoggerFactory.getLogger(PaymentLineItemGrpcServiceImpl.class);
  private final PaymentLineItemService paymentLineItemService;

  @Autowired
  public PaymentLineItemGrpcServiceImpl(PaymentLineItemService paymentLineItemService) {
    this.paymentLineItemService = paymentLineItemService;
  }

  @Override
  public void getPaymentLineItemsForJob(GetPaymentLineItemsForJobRequest request,
      StreamObserver<PaymentLineItemListResponse> responseObserver) {
    try {
      List<PaymentLineItemDTO> paymentLineItems = paymentLineItemService.getPaymentLineItemsForJob(request.getJobUid());

      PaymentLineItemListResponse response = PaymentLineItemListResponse.newBuilder()
          .addAllPaymentLineItems(
              paymentLineItems.stream().map(this::convertDtoToPaymentLineItem).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void getPaymentLineItemsForTimelog(GetPaymentLineItemsForTimelogRequest request,
      StreamObserver<PaymentLineItemListResponse> responseObserver) {
    try {
      List<PaymentLineItemDTO> paymentLineItems = paymentLineItemService
          .getPaymentLineItemsForTimelog(request.getTimelogUid());

      PaymentLineItemListResponse response = PaymentLineItemListResponse.newBuilder()
          .addAllPaymentLineItems(
              paymentLineItems.stream().map(this::convertDtoToPaymentLineItem).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void getPaymentLineItemsForContractor(GetPaymentLineItemsForContractorRequest request,
      StreamObserver<PaymentLineItemListResponse> responseObserver) {
    try {
      List<PaymentLineItemDTO> paymentLineItems = paymentLineItemService.getPaymentLineItemsForContractor(
          request.getContractorId(),
          request.getStartTime(),
          request.getEndTime());

      PaymentLineItemListResponse response = PaymentLineItemListResponse.newBuilder()
          .addAllPaymentLineItems(
              paymentLineItems.stream().map(this::convertDtoToPaymentLineItem).collect(Collectors.toList()))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void markAsPaid(MarkAsPaidRequest request, StreamObserver<PaymentLineItemResponse> responseObserver) {
    try {
      if (request.getId() == null || request.getId().isEmpty()) {
        throw new SCDException("Payment line item ID is required", "MISSING_PAYMENT_ID");
      }

      PaymentLineItemDTO paymentLineItem = paymentLineItemService.markAsPaid(request.getId());

      PaymentLineItemResponse response = PaymentLineItemResponse.newBuilder()
          .setPaymentLineItem(convertDtoToPaymentLineItem(paymentLineItem))
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (EntityNotFoundException e) {
      logger.warn("Payment line item not found: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.NOT_FOUND,
          "Payment line item not found: " + e.getMessage(),
          e));
    } catch (SCDException e) {
      logger.warn("Invalid argument in markAsPaid: {}", e.getMessage());
      responseObserver.onError(createDetailedError(
          Status.INVALID_ARGUMENT,
          e.getMessage(),
          e));
    } catch (Exception e) {
      logger.error("Error in markAsPaid call", e);
      responseObserver.onError(createDetailedError(
          Status.INTERNAL,
          "Internal error during marking payment as paid: " + e.getMessage(),
          e));
    }
  }

  @Override
  public void getTotalAmountForContractor(GetTotalAmountForContractorRequest request,
      StreamObserver<TotalAmountResponse> responseObserver) {
    try {
      Double totalAmount = paymentLineItemService.getTotalAmountForContractor(
          request.getContractorId(),
          request.getStartTime(),
          request.getEndTime());

      TotalAmountResponse response = TotalAmountResponse.newBuilder()
          .setTotalAmount(totalAmount)
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }

  /**
   * Convert a PaymentLineItemDTO to a gRPC PaymentLineItem
   *
   * @param dto The PaymentLineItemDTO to convert
   * @return The gRPC PaymentLineItem
   */
  private PaymentLineItem convertDtoToPaymentLineItem(PaymentLineItemDTO dto) {
    return PaymentLineItem.newBuilder()
        .setId(dto.getId())
        .setVersion(dto.getVersion())
        .setUid(dto.getUid())
        .setCreatedAt(dto.getCreatedAt().getTime())
        .setUpdatedAt(dto.getUpdatedAt().getTime())
        .setJobUid(dto.getJobUid())
        .setTimelogUid(dto.getTimelogUid())
        .setAmount(dto.getAmount().doubleValue())
        .setStatus(dto.getStatus())
        .build();
  }

  /**
   * Convert a gRPC PaymentLineItem to a PaymentLineItemDTO
   *
   * @param paymentLineItem The gRPC PaymentLineItem to convert
   * @return The PaymentLineItemDTO
   */
  private PaymentLineItemDTO convertPaymentLineItemToDto(PaymentLineItem paymentLineItem) {
    return PaymentLineItemDTO.builder()
        .id(paymentLineItem.getId())
        .version(paymentLineItem.getVersion())
        .uid(paymentLineItem.getUid())
        .createdAt(new Date(paymentLineItem.getCreatedAt()))
        .updatedAt(new Date(paymentLineItem.getUpdatedAt()))
        .jobUid(paymentLineItem.getJobUid())
        .timelogUid(paymentLineItem.getTimelogUid())
        .amount(BigDecimal.valueOf(paymentLineItem.getAmount()))
        .status(paymentLineItem.getStatus())
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