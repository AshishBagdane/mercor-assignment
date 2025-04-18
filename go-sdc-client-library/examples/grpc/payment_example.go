package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/mercor-ai/go-sdc-client-library/pkg/client"
	scdGorm "github.com/mercor-ai/go-sdc-client-library/pkg/gorm"
	"github.com/mercor-ai/go-sdc-client-library/pkg/model"
	"gorm.io/driver/postgres"
)

// PaymentLineItemEntity represents a payment entity
type PaymentLineItemEntity struct {
	model.SCDModel
	JobUID     string  `json:"job_uid"`     // Foreign Key to jobs.uid
	TimelogUID string  `json:"timelog_uid"` // Foreign Key to timelogs.uid
	Amount     float64 `json:"amount"`      // Decimal (10,2)
	Status     string  `json:"status"`      // Enum: "pending", "paid"
}

// TableName specifies the table name for PaymentLineItem
func (PaymentLineItemEntity) TableName() string {
	return "payment_line_items"
}

// EntityType returns the SCD entity type
func (PaymentLineItemEntity) EntityType() string {
	return "payment_line_item"
}

// Run the payment line item examples
func runPaymentExamples() {
	// Get environment variables for configuration
	scdHost := getEnvVar("SCD_HOST", "localhost")
	scdPort := 9090 // Default SCD service port
	dbHost := getEnvVar("DB_HOST", "localhost")
	dbPort := getEnvVar("DB_PORT", "5432")
	dbUser := getEnvVar("DB_USER", "user")
	dbPass := getEnvVar("DB_PASSWORD", "password")
	dbName := getEnvVar("DB_NAME", "employment")
	sslMode := getEnvVar("DB_SSL_MODE", "disable")

	// Initialize the SCD client
	scdClient, err := client.NewClientWithHostPort(scdHost, scdPort)
	if err != nil {
		log.Fatalf("Failed to create SCD client: %v", err)
	}
	defer scdClient.Close()

	// Create PostgreSQL connection string
	dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=%s TimeZone=UTC",
		dbHost, dbUser, dbPass, dbName, dbPort, sslMode)

	// Initialize GORM with SCD plugin
	db, err := scdGorm.Open(postgres.Open(dsn), &scdGorm.Config{
		SCDClient: scdClient,
	})
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	fmt.Println("\n===== PAYMENT LINE ITEM OPERATIONS EXAMPLE =====")

	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	// Direct Client API Examples
	fmt.Println("\n=== DIRECT CLIENT API EXAMPLES ===")
	demoDirectPaymentOperations(ctx, scdClient)

	// GORM Integration Examples
	fmt.Println("\n=== GORM INTEGRATION EXAMPLES ===")
	demoGormPaymentOperations(db)
}

// Helper function to get environment variable with fallback
func getEnvVar(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}

// RunPaymentExamples is the exported function that will be called from main.go
func RunPaymentExamples() {
	runPaymentExamples()
}

func demoDirectPaymentOperations(ctx context.Context, c *client.Client) {
	// Example 1: Get payment line items for a job
	fmt.Println("Querying payment line items for a job...")
	jobUID := "job_uid_ywij5sh1tvfp5nkq7azav"
	conditions := map[string]interface{}{
		"job_uid": jobUID,
	}
	options := client.QueryOptions{
		LatestVersionOnly: true,
		SortBy:            "amount",
		SortDirection:     "desc",
	}
	payments, err := c.Query(ctx, "payment_line_item", conditions, options)
	if err != nil {
		fmt.Printf("Error querying payments: %v\n", err)
	} else {
		fmt.Printf("Found %d payment line items for job %s:\n", len(payments), jobUID)
		totalAmount := 0.0
		for i, p := range payments {
			amount := 0.0
			if a, ok := p["amount"].(float64); ok {
				amount = a
				totalAmount += amount
			}

			fmt.Printf("  Payment %d: ID=%s, Amount=$%.2f, Status=%v\n",
				i+1, p["id"], amount, p["status"])
		}
		fmt.Printf("  Total Amount: $%.2f\n", totalAmount)
	}

	// Example 2: Get payment line items for a contractor
	fmt.Println("\nQuerying payment line items for a contractor...")
	contractorID := "cont_e0nhseq682vkoc4d"

	// First we need to get the contractor's jobs
	jobConditions := map[string]interface{}{
		"contractor_id": contractorID,
	}
	jobOptions := client.QueryOptions{
		LatestVersionOnly: true,
	}
	contractorJobs, err := c.Query(ctx, client.EntityTypeJob, jobConditions, jobOptions)
	if err != nil {
		fmt.Printf("Error querying jobs: %v\n", err)
		return
	}

	fmt.Printf("Found %d jobs for contractor %s\n", len(contractorJobs), contractorID)
	if len(contractorJobs) == 0 {
		return
	}

	// Now get payments for the first job
	jobUID = contractorJobs[0]["uid"].(string)
	fmt.Printf("Getting payments for job %s\n", jobUID)

	paymentConditions := map[string]interface{}{
		"job_uid": jobUID,
	}
	contractorPayments, err := c.Query(ctx, "payment_line_item", paymentConditions, options)
	if err != nil {
		fmt.Printf("Error querying payments: %v\n", err)
	} else {
		fmt.Printf("Found %d payment line items:\n", len(contractorPayments))
		for i, p := range contractorPayments {
			amount := 0.0
			if a, ok := p["amount"].(float64); ok {
				amount = a
			}

			fmt.Printf("  Payment %d: ID=%s, Amount=$%.2f, Status=%v\n",
				i+1, p["id"], amount, p["status"])
		}
	}

	// Example 3: Get payment line items for a timelog
	fmt.Println("\nQuerying payment line items for a timelog...")
	timelogUID := "tl_uid_AAABk__7Gd3uvJs1nkk5Ir6k"
	timelogConditions := map[string]interface{}{
		"timelog_uid": timelogUID,
	}
	timelogPayments, err := c.Query(ctx, "payment_line_item", timelogConditions, options)
	if err != nil {
		fmt.Printf("Error querying payments: %v\n", err)
	} else {
		fmt.Printf("Found %d payment line items for timelog %s:\n", len(timelogPayments), timelogUID)
		for i, p := range timelogPayments {
			amount := 0.0
			if a, ok := p["amount"].(float64); ok {
				amount = a
			}

			fmt.Printf("  Payment %d: ID=%s, Amount=$%.2f, Status=%v\n",
				i+1, p["id"], amount, p["status"])
		}
	}

	// Example 4: Get payment line items for a specific period
	fmt.Println("\nQuerying payment line items for March 2023...")
	startTime := time.Date(2023, 3, 1, 0, 0, 0, 0, time.UTC).Unix()
	endTime := time.Date(2023, 3, 31, 23, 59, 59, 999999999, time.UTC).Unix()

	periodConditions := map[string]interface{}{
		"created_at >=": startTime,
		"created_at <=": endTime,
	}
	periodPayments, err := c.Query(ctx, "payment_line_item", periodConditions, options)
	if err != nil {
		fmt.Printf("Error querying payments: %v\n", err)
	} else {
		fmt.Printf("Found %d payment line items for March 2023:\n", len(periodPayments))
		totalAmount := 0.0
		for i, p := range periodPayments {
			amount := 0.0
			if a, ok := p["amount"].(float64); ok {
				amount = a
				totalAmount += amount
			}

			fmt.Printf("  Payment %d: ID=%s, Amount=$%.2f, Status=%v\n",
				i+1, p["id"], amount, p["status"])
		}
		fmt.Printf("  Total Amount: $%.2f\n", totalAmount)
	}

	// Example 5: Mark a payment as paid
	fmt.Println("\nMarking a payment as paid...")
	// Find a pending payment first
	pendingConditions := map[string]interface{}{
		"status": "pending",
	}
	pendingPayments, err := c.Query(ctx, "payment_line_item", pendingConditions, options)
	if err != nil {
		fmt.Printf("Error querying pending payments: %v\n", err)
		return
	}

	if len(pendingPayments) == 0 {
		fmt.Println("No pending payments found")
		return
	}

	// Get first pending payment
	pendingPayment := pendingPayments[0]
	paymentID := pendingPayment["id"].(string)
	currentVersion := int32(1)
	if v, ok := pendingPayment["version"].(int32); ok {
		currentVersion = v
	}

	// Update it to paid
	updatedPayment := map[string]interface{}{
		"id":      paymentID,
		"version": currentVersion,
		"status":  "paid",
	}

	result, err := c.Update(ctx, "payment_line_item", updatedPayment)
	if err != nil {
		fmt.Printf("Error updating payment: %v\n", err)
	} else {
		amount := 0.0
		if a, ok := result["amount"].(float64); ok {
			amount = a
		}

		fmt.Printf("Payment marked as paid: ID=%s, Version=%v, Amount=$%.2f, Status=%v\n",
			result["id"], result["version"], amount, result["status"])
	}

	// Example 6: Batch get payment line items
	fmt.Println("\nPerforming batch get operation...")
	// Get a few payment IDs first
	var paymentIDs []string
	for i, p := range payments {
		if i >= 2 {
			break
		}
		paymentIDs = append(paymentIDs, p["id"].(string))
	}

	if len(paymentIDs) == 0 {
		fmt.Println("No payment IDs available for batch get")
		return
	}

	batchResult, err := c.BatchGet(ctx, "payment_line_item", paymentIDs)
	if err != nil {
		fmt.Printf("Error in batch get: %v\n", err)
	} else {
		fmt.Printf("Batch get result has %d items:\n", len(batchResult))
		for id, data := range batchResult {
			amount := 0.0
			if a, ok := data["amount"].(float64); ok {
				amount = a
			}

			fmt.Printf("  Payment %s: Version=%v, Amount=$%.2f, Status=%v\n",
				id, data["version"], amount, data["status"])
		}
	}
}

func demoGormPaymentOperations(db *scdGorm.DB) {
	// Access the underlying SCD client
	scdClient := db.Client()
	ctx := context.Background()

	// Example 1: Create a new payment line item
	fmt.Println("Creating a new payment line item...")
	// First find a job and timelog to associate this payment with
	var jobUID string = "job_uid_ywij5sh1tvfp5nkq7azav" // Default if we can't find one
	var timelogUID string = ""                          // Default empty

	// Use Query API to find a job
	jobConditions := map[string]interface{}{
		"uid": "job_uid_ywij5sh1tvfp5nkq7azav", // Try to find this specific job
	}
	jobOptions := client.QueryOptions{
		LatestVersionOnly: true,
		Limit:             1,
	}

	jobResults, err := scdClient.Query(ctx, client.EntityTypeJob, jobConditions, jobOptions)
	if err == nil && len(jobResults) > 0 {
		if uid, ok := jobResults[0]["uid"].(string); ok {
			jobUID = uid
		}
	}

	// Use Query API to find a timelog
	timelogConditions := map[string]interface{}{
		"job_uid": jobUID,
	}
	timelogOptions := client.QueryOptions{
		LatestVersionOnly: true,
		Limit:             1,
	}

	timelogResults, err := scdClient.Query(ctx, "timelog", timelogConditions, timelogOptions)
	if err == nil && len(timelogResults) > 0 {
		if uid, ok := timelogResults[0]["uid"].(string); ok {
			timelogUID = uid
		}
	}

	// Create payment with unique ID and UID
	paymentID := fmt.Sprintf("pmt_%s", generateUniqueID())
	newPayment := PaymentLineItemEntity{
		SCDModel: model.SCDModel{
			ID:      paymentID,
			Version: 1,
			UID:     fmt.Sprintf("pmt_uid_%s", generateUniqueID()),
		},
		JobUID:     jobUID,
		TimelogUID: timelogUID,
		Amount:     10000, // $100.00
		Status:     "pending",
	}

	if result := db.Create(&newPayment); result.Error != nil {
		fmt.Printf("Error creating payment: %v\n", result.Error)
	} else {
		fmt.Printf("Payment created: ID=%s, Version=%d, UID=%s\n",
			newPayment.ID, newPayment.Version, newPayment.UID)
		fmt.Printf("  Amount: $%.2f, Status: %s\n",
			float64(newPayment.Amount)/100, newPayment.Status)
	}

	// Example 2: Find payments for a job
	fmt.Println("\nFinding payments for a job...")

	// Use direct gRPC call to Query instead of SQL
	paymentConditions := map[string]interface{}{
		"job_uid": jobUID,
	}
	options := client.QueryOptions{
		LatestVersionOnly: true,
		SortBy:            "amount",
		SortDirection:     "desc",
	}

	payments, err := scdClient.Query(ctx, "payment_line_item", paymentConditions, options)
	if err != nil {
		fmt.Printf("Error finding payments: %v\n", err)
	} else {
		fmt.Printf("Found %d payments for job %s:\n", len(payments), jobUID)
		totalAmount := int64(0)

		for i, p := range payments {
			amount := int64(0)
			if a, ok := p["amount"].(int64); ok {
				amount = a
				totalAmount += amount
			}

			fmt.Printf("  Payment %d: ID=%s, Amount=$%.2f, Status=%s\n",
				i+1, p["id"], float64(amount)/100, p["status"])
		}
		fmt.Printf("  Total Amount: $%.2f\n", float64(totalAmount)/100)
	}

	// Example 3: Process a payment
	if len(payments) > 0 {
		fmt.Println("\nProcessing payment...")
		payment := payments[0]
		paymentID := payment["id"].(string)
		currentVersion := int32(1)
		if v, ok := payment["version"].(int32); ok {
			currentVersion = v
		}

		// Update using the Update gRPC method
		updatedPayment := map[string]interface{}{
			"id":      paymentID,
			"version": currentVersion,
			"status":  "processed",
		}

		result, err := scdClient.Update(ctx, "payment_line_item", updatedPayment)
		if err != nil {
			fmt.Printf("Error processing payment: %v\n", err)
		} else {
			fmt.Printf("Payment processed: ID=%s, Version=%v, Status=%s\n",
				result["id"], result["version"], result["status"])
		}
	}

	// Example 4: Find payments with specific status
	fmt.Println("\nFinding payments with 'processed' status...")

	// Use direct gRPC call to Query with status condition
	statusConditions := map[string]interface{}{
		"status": "processed",
	}

	processedPayments, err := scdClient.Query(ctx, "payment_line_item", statusConditions, options)
	if err != nil {
		fmt.Printf("Error finding payments: %v\n", err)
	} else {
		fmt.Printf("Found %d processed payments:\n", len(processedPayments))
		for i, p := range processedPayments {
			amount := int64(0)
			if a, ok := p["amount"].(int64); ok {
				amount = a
			}

			fmt.Printf("  Payment %d: ID=%s, Amount=$%.2f\n",
				i+1, p["id"], float64(amount)/100)
		}
	}

	// Example 5: Find payments above amount threshold
	fmt.Println("\nFinding payments above $50.00...")
	threshold := int64(5000) // $50.00

	// Use direct gRPC call to Query with amount threshold
	amountConditions := map[string]interface{}{
		"amount >": threshold,
	}

	largePayments, err := scdClient.Query(ctx, "payment_line_item", amountConditions, options)
	if err != nil {
		fmt.Printf("Error finding payments: %v\n", err)
	} else {
		fmt.Printf("Found %d payments above $%.2f:\n", len(largePayments), float64(threshold)/100)
		for i, p := range largePayments {
			amount := int64(0)
			if a, ok := p["amount"].(int64); ok {
				amount = a
			}

			fmt.Printf("  Payment %d: ID=%s, Amount=$%.2f, Status=%s\n",
				i+1, p["id"], float64(amount)/100, p["status"])
		}
	}

	// Example 6: Version history for a specific payment
	if len(payments) > 0 {
		fmt.Println("\nQuerying version history for a payment...")
		paymentID := payments[0]["id"].(string)

		// Use direct gRPC call to GetVersionHistory instead of SQL
		versions, err := scdClient.GetVersionHistory(ctx, "payment_line_item", paymentID)
		if err != nil {
			fmt.Printf("Error finding payment versions: %v\n", err)
		} else {
			fmt.Printf("Found %d versions for payment %s:\n", len(versions), paymentID)
			for i, v := range versions {
				amount := int64(0)
				if a, ok := v["amount"].(int64); ok {
					amount = a
				}

				fmt.Printf("  Version %d: UID=%s, Version=%v, Status=%s, Amount=$%.2f\n",
					i+1, v["uid"], v["version"], v["status"], float64(amount)/100)
			}
		}
	}
}
