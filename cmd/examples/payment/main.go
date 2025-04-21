// cmd/examples/payment/main.go
package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"

	"github.com/mercor-ai/scd-go-client/pkg/client"
	"github.com/mercor-ai/scd-go-client/pkg/config"
	"github.com/mercor-ai/scd-go-client/pkg/repository"
)

func main() {
	// Define command line flags
	paymentID := flag.String("id", "", "Payment Line Item ID to query (required)")
	contractorID := flag.String("contractor", "", "Contractor ID for total amount query (optional)")
	startTimeStr := flag.String("start", "", "Start time for period queries in RFC3339 format (optional)")
	endTimeStr := flag.String("end", "", "End time for period queries in RFC3339 format (optional)")

	flag.Parse()

	// Validate required parameters
	if *paymentID == "" {
		flag.PrintDefaults()
		fmt.Println("\nError: Payment Line Item ID is required")
		os.Exit(1)
	}

	// Parse time parameters if provided
	var startTime, endTime int64
	if *startTimeStr != "" {
		t, err := time.Parse(time.RFC3339, *startTimeStr)
		if err != nil {
			fmt.Printf("Invalid start time format: %v\n", err)
			os.Exit(1)
		}
		startTime = t.UnixNano() / 1000000
	} else {
		// Default to 1 month ago
		startTime = time.Now().AddDate(0, -1, 0).UnixNano() / 1000000
	}

	if *endTimeStr != "" {
		t, err := time.Parse(time.RFC3339, *endTimeStr)
		if err != nil {
			fmt.Printf("Invalid end time format: %v\n", err)
			os.Exit(1)
		}
		endTime = t.UnixNano() / 1000000
	} else {
		// Default to now
		endTime = time.Now().UnixNano() / 1000000
	}

	// Load configuration
	cfg := config.DefaultConfig()

	// Connect to database
	db, err := gorm.Open(postgres.Open(cfg.Database.GetDSN()), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	// Create SCD client
	scdClient, err := client.New(client.Config{
		ServerAddress: cfg.GRPC.GetServerAddress(),
		DialOptions:   cfg.GRPC.DialOptions,
		Timeout:       cfg.GRPC.Timeout,
	})
	if err != nil {
		log.Fatalf("Failed to create SCD client: %v", err)
	}
	defer scdClient.Close()

	// Create payment repository
	paymentRepo := repository.NewPaymentRepository(db, scdClient)

	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	// Example 1: Get the latest version of the existing payment line item
	fmt.Printf("Getting latest version of payment line item with ID %s...\n", *paymentID)

	existingPayment, err := paymentRepo.GetPaymentLineItemRemote(ctx, *paymentID)
	if err != nil {
		log.Fatalf("Failed to get payment line item from SCD service: %v", err)
	}

	fmt.Printf("Retrieved payment: ID=%s, Version=%d, UID=%s, Amount=%.2f, Status=%s\n",
		existingPayment.ID, existingPayment.Version, existingPayment.UID,
		existingPayment.Amount, existingPayment.Status)

	// Example 2: Mark payment as paid through SCD service
	if existingPayment.Status != "paid" {
		fmt.Println("\nMarking payment as paid through SCD service...")

		paidPayment, err := paymentRepo.MarkAsPaidRemote(ctx, *paymentID)
		if err != nil {
			log.Fatalf("Failed to mark payment as paid through SCD service: %v", err)
		}

		fmt.Printf("Updated payment: ID=%s, Version=%d, UID=%s, Status=%s (changed from %s)\n",
			paidPayment.ID, paidPayment.Version, paidPayment.UID,
			paidPayment.Status, existingPayment.Status)
	} else {
		fmt.Println("\nPayment is already marked as paid.")
	}

	// Example 3: Get payment line items for the job associated with this payment
	fmt.Println("\nGetting payment line items for the associated job...")
	jobPayments, err := paymentRepo.GetPaymentLineItemsForJobRemote(ctx, existingPayment.JobUID)
	if err != nil {
		log.Fatalf("Failed to get job payments: %v", err)
	}

	fmt.Printf("Found %d payment line items for job %s:\n", len(jobPayments), existingPayment.JobUID)
	for _, payment := range jobPayments {
		fmt.Printf("- ID=%s, Version=%d, UID=%s, Amount=%.2f, Status=%s\n",
			payment.ID, payment.Version, payment.UID, payment.Amount, payment.Status)
	}

	// Example 4: Get payment line item version history from SCD service
	fmt.Println("\nGetting payment history from SCD service...")
	paymentHistory, err := paymentRepo.GetPaymentHistoryRemote(ctx, *paymentID)
	if err != nil {
		log.Fatalf("Failed to get payment history: %v", err)
	}

	fmt.Printf("Payment history for %s has %d versions:\n", *paymentID, len(paymentHistory))
	for _, version := range paymentHistory {
		fmt.Printf("- Version=%d, UID=%s, Status=%s, Amount=%.2f, Created=%s\n",
			version.Version, version.UID, version.Status, version.Amount,
			version.CreatedAt.Format(time.RFC3339))
	}

	// Example 5: Get total amount for the contractor
	// Use provided contractor ID or extract from the job if not provided
	usedContractorID := *contractorID
	if usedContractorID == "" {
		// In a real implementation, you'd get the contractor ID from the job
		// This is just a placeholder
		usedContractorID = "cont_example456"
		fmt.Println("\nNo contractor ID provided, using default example ID.")
	}

	fmt.Printf("\nGetting total amount for contractor %s...\n", usedContractorID)
	totalAmount, err := paymentRepo.GetTotalAmountForContractorRemote(ctx, usedContractorID, startTime, endTime)
	if err != nil {
		log.Fatalf("Failed to get total amount: %v", err)
	}

	startTimeFormatted := time.Unix(0, startTime*1000000).Format(time.RFC3339)
	endTimeFormatted := time.Unix(0, endTime*1000000).Format(time.RFC3339)

	fmt.Printf("Total amount for contractor %s from %s to %s: $%.2f\n",
		usedContractorID, startTimeFormatted, endTimeFormatted, totalAmount)
}
