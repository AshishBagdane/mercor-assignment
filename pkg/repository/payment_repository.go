// pkg/repository/payment_repository.go
package repository

import (
	"context"
	"fmt"
	"time"

	"gorm.io/gorm"

	"github.com/mercor-ai/scd-go-client/api/common"
	"github.com/mercor-ai/scd-go-client/api/core"
	"github.com/mercor-ai/scd-go-client/api/paymentlineitems"
	"github.com/mercor-ai/scd-go-client/pkg/client"
	"github.com/mercor-ai/scd-go-client/pkg/models"
	paymentmodel "github.com/mercor-ai/scd-go-client/pkg/models/payment"
)

// PaymentRepository handles database operations for payment line items
type PaymentRepository struct {
	BaseRepository
	paymentClient paymentlineitems.PaymentLineItemServiceClient
	coreClient    core.SCDServiceClient
}

// NewPaymentRepository creates a new payment repository
func NewPaymentRepository(db *gorm.DB, scdClient *client.Client) *PaymentRepository {
	return &PaymentRepository{
		BaseRepository: BaseRepository{
			DB:         db,
			SCDClient:  scdClient,
			EntityType: "payment_line_items",
		},
		paymentClient: scdClient.Payment(),
		coreClient:    scdClient.Core(),
	}
}

// GetPaymentLineItemRemote retrieves a payment line item by ID from the SCD service
func (r *PaymentRepository) GetPaymentLineItemRemote(ctx context.Context, id string) (*paymentmodel.PaymentLineItem, error) {
	// Call SCD service to get latest version
	resp, err := r.coreClient.GetLatestVersion(ctx, &core.GetLatestVersionRequest{
		EntityType: "payment_line_items",
		Id:         id,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get payment line item from SCD service: %w", err)
	}

	// Convert response to model
	payment := convertEntityToPayment(resp.Entity)

	// Save to local database for caching
	if err := r.DB.Create(payment).Error; err != nil {
		// Log error but continue - we still have the remote data
		fmt.Printf("Warning: Failed to cache payment in local database: %v\n", err)
	}

	return payment, nil
}

// MarkAsPaidRemote marks a payment as paid through the SCD service
func (r *PaymentRepository) MarkAsPaidRemote(ctx context.Context, id string) (*paymentmodel.PaymentLineItem, error) {
	// Call SCD service to mark payment as paid
	resp, err := r.paymentClient.MarkAsPaid(ctx, &paymentlineitems.MarkAsPaidRequest{
		Id: id,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to mark payment as paid through SCD service: %w", err)
	}

	// Convert response to model
	paidPayment := convertPaymentProtoToModel(resp.PaymentLineItem)

	// Save to local database for caching
	if err := r.DB.Create(paidPayment).Error; err != nil {
		// Log error but continue - we still have the remote data
		fmt.Printf("Warning: Failed to cache paid payment in local database: %v\n", err)
	}

	return paidPayment, nil
}

// GetPaymentLineItemsForJobRemote retrieves payment line items for a job from the SCD service
func (r *PaymentRepository) GetPaymentLineItemsForJobRemote(ctx context.Context, jobUID string) ([]*paymentmodel.PaymentLineItem, error) {
	// Call SCD service to get payment line items for job
	resp, err := r.paymentClient.GetPaymentLineItemsForJob(ctx, &paymentlineitems.GetPaymentLineItemsForJobRequest{
		JobUid: jobUID,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get payment line items for job from SCD service: %w", err)
	}

	// Convert to model payment line items
	payments := make([]*paymentmodel.PaymentLineItem, 0, len(resp.PaymentLineItems))
	for _, pliProto := range resp.PaymentLineItems {
		payments = append(payments, convertPaymentProtoToModel(pliProto))
	}

	// Save to local database for caching
	if len(payments) > 0 {
		if err := r.DB.CreateInBatches(payments, 100).Error; err != nil {
			// Log error but continue - we still have the remote data
			fmt.Printf("Warning: Failed to cache payment line items in local database: %v\n", err)
		}
	}

	return payments, nil
}

// GetPaymentHistoryRemote retrieves payment line item version history from the SCD service
func (r *PaymentRepository) GetPaymentHistoryRemote(ctx context.Context, id string) ([]*paymentmodel.PaymentLineItem, error) {
	// Call SCD service to get payment history
	resp, err := r.coreClient.GetVersionHistory(ctx, &core.GetVersionHistoryRequest{
		EntityType: "payment_line_items",
		Id:         id,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get payment history from SCD service: %w", err)
	}

	// Convert to model payment line items
	payments := make([]*paymentmodel.PaymentLineItem, 0, len(resp.Entities))
	for _, entity := range resp.Entities {
		payments = append(payments, convertEntityToPayment(entity))
	}

	// Save to local database for caching
	if len(payments) > 0 {
		if err := r.DB.CreateInBatches(payments, 100).Error; err != nil {
			// Log error but continue - we still have the remote data
			fmt.Printf("Warning: Failed to cache payment history in local database: %v\n", err)
		}
	}

	return payments, nil
}

// GetTotalAmountForContractorRemote gets the total amount for a contractor in a time period
func (r *PaymentRepository) GetTotalAmountForContractorRemote(ctx context.Context, contractorID string, startTime, endTime int64) (float64, error) {
	// Call SCD service to get total amount
	resp, err := r.paymentClient.GetTotalAmountForContractor(ctx, &paymentlineitems.GetTotalAmountForContractorRequest{
		ContractorId: contractorID,
		StartTime:    startTime,
		EndTime:      endTime,
	})
	if err != nil {
		return 0, fmt.Errorf("failed to get total amount from SCD service: %w", err)
	}

	return resp.TotalAmount, nil
}

// Helper functions

// convertPaymentProtoToModel converts a payment proto to a payment model
func convertPaymentProtoToModel(paymentProto *paymentlineitems.PaymentLineItemProto) *paymentmodel.PaymentLineItem {
	return &paymentmodel.PaymentLineItem{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        paymentProto.Id,
			Version:   int(paymentProto.Version),
			UID:       paymentProto.Uid,
			CreatedAt: time.Unix(0, paymentProto.CreatedAt*1000000),
			UpdatedAt: time.Unix(0, paymentProto.UpdatedAt*1000000),
		},
		JobUID:     paymentProto.JobUid,
		TimelogUID: paymentProto.TimelogUid,
		Amount:     paymentProto.Amount,
		Status:     paymentProto.Status,
	}
}

// convertEntityToPayment converts a generic entity to a payment model
func convertEntityToPayment(entity *common.Entity) *paymentmodel.PaymentLineItem {
	// In a real implementation, parse the entity.Data to get payment-specific fields
	// For now, create a payment with basic SCD fields set
	return &paymentmodel.PaymentLineItem{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        entity.Id,
			Version:   int(entity.Version),
			UID:       entity.Uid,
			CreatedAt: time.Unix(0, entity.CreatedAt*1000000),
			UpdatedAt: time.Unix(0, entity.UpdatedAt*1000000),
		},
		// Payment-specific fields would come from parsing entity.Data
		JobUID:     "",        // Placeholder
		TimelogUID: "",        // Placeholder
		Amount:     0.0,       // Placeholder
		Status:     "unknown", // Placeholder
	}
}

// GetPaymentLineItemsForTimelogRemote retrieves payment line items for a timelog from the SCD service
func (r *PaymentRepository) GetPaymentLineItemsForTimelogRemote(ctx context.Context, timelogUID string) ([]*paymentmodel.PaymentLineItem, error) {
	// Call SCD service to get payment line items for timelog
	resp, err := r.paymentClient.GetPaymentLineItemsForTimelog(ctx, &paymentlineitems.GetPaymentLineItemsForTimelogRequest{
		TimelogUid: timelogUID,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get payment line items for timelog from SCD service: %w", err)
	}

	// Convert to model payment line items
	payments := make([]*paymentmodel.PaymentLineItem, 0, len(resp.PaymentLineItems))
	for _, pliProto := range resp.PaymentLineItems {
		payments = append(payments, convertPaymentProtoToModel(pliProto))
	}

	// Save to local database for caching
	if len(payments) > 0 {
		if err := r.DB.CreateInBatches(payments, 100).Error; err != nil {
			// Log error but continue - we still have the remote data
			fmt.Printf("Warning: Failed to cache payment line items in local database: %v\n", err)
		}
	}

	return payments, nil
}

// GetPaymentLineItemsForContractorRemote retrieves payment line items for a contractor in a time period
func (r *PaymentRepository) GetPaymentLineItemsForContractorRemote(ctx context.Context, contractorID string, startTime, endTime int64) ([]*paymentmodel.PaymentLineItem, error) {
	// Call SCD service to get payment line items for contractor
	resp, err := r.paymentClient.GetPaymentLineItemsForContractor(ctx, &paymentlineitems.GetPaymentLineItemsForContractorRequest{
		ContractorId: contractorID,
		StartTime:    startTime,
		EndTime:      endTime,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get payment line items for contractor from SCD service: %w", err)
	}

	// Convert to model payment line items
	payments := make([]*paymentmodel.PaymentLineItem, 0, len(resp.PaymentLineItems))
	for _, pliProto := range resp.PaymentLineItems {
		payments = append(payments, convertPaymentProtoToModel(pliProto))
	}

	// Save to local database for caching
	if len(payments) > 0 {
		if err := r.DB.CreateInBatches(payments, 100).Error; err != nil {
			// Log error but continue - we still have the remote data
			fmt.Printf("Warning: Failed to cache payment line items in local database: %v\n", err)
		}
	}

	return payments, nil
}
