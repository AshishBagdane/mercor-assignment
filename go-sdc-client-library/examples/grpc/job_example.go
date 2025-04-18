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

// JobEntity represents a job entity implementing the Versioned interface
type JobEntity struct {
	model.SCDModel         // Embed SCD fields
	Status         string  `json:"status"`        // Enum: "extended", "active", "completed"
	Rate           float64 `json:"rate"`          // Decimal (10,2)
	Title          string  `json:"title"`         // String
	CompanyID      string  `json:"company_id"`    // Foreign Key
	ContractorID   string  `json:"contractor_id"` // Foreign Key
}

// TableName specifies the table name for JobEntity
func (JobEntity) TableName() string {
	return "jobs"
}

// EntityType returns the SCD entity type for the Job model
func (JobEntity) EntityType() string {
	return "job"
}

// RunJobExamples is the exported function that will be called from main.go
func RunJobExamples() {
	// Get environment variables for configuration
	scdHost := getEnvForJob("SCD_HOST", "localhost")
	scdPort := 9090 // Default SCD service port
	dbHost := getEnvForJob("DB_HOST", "localhost")
	dbPort := getEnvForJob("DB_PORT", "5432")
	dbUser := getEnvForJob("DB_USER", "user")
	dbPass := getEnvForJob("DB_PASSWORD", "password")
	dbName := getEnvForJob("DB_NAME", "employment")
	sslMode := getEnvForJob("DB_SSL_MODE", "disable")

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

	fmt.Println("\n===== JOB OPERATIONS EXAMPLE =====")

	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	// Direct Client API Examples
	fmt.Println("\n=== DIRECT CLIENT API EXAMPLES ===")
	demoDirectJobOperations(ctx, scdClient)

	// GORM Integration Examples
	fmt.Println("\n=== GORM INTEGRATION EXAMPLES ===")
	demoGormJobOperations(db)
}

// Helper function to get environment variable with fallback
func getEnvForJob(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}

func demoDirectJobOperations(ctx context.Context, c *client.Client) {
	fmt.Println("Connecting to SCD service at:", c.GetTarget())
	fmt.Println("This example is now using real gRPC calls instead of mock data.")

	// Example 1: Get latest version of a job
	jobID := "job_ckbk6oo4hn7pacdgcz9f"
	fmt.Printf("\nGetting latest version of job %s...\n", jobID)
	jobData, err := c.GetLatestVersion(ctx, client.EntityTypeJob, jobID)
	if err != nil {
		fmt.Printf("Error getting job: %v\n", err)
	} else {
		fmt.Printf("Latest job version: ID=%s, Version=%v, Status=%v\n",
			jobData["id"], jobData["version"], jobData["status"])

		// Print all fields in the job
		fmt.Println("All job fields:")
		for k, v := range jobData {
			fmt.Printf("  %s: %v\n", k, v)
		}
	}

	// Example 2: Get version history of a job
	fmt.Printf("\nGetting version history of job %s...\n", jobID)
	jobVersions, err := c.GetVersionHistory(ctx, client.EntityTypeJob, jobID)
	if err != nil {
		fmt.Printf("Error getting job history: %v\n", err)
	} else {
		fmt.Printf("Found %d versions:\n", len(jobVersions))
		for i, v := range jobVersions {
			fmt.Printf("  Version %d: ID=%s, Version=%v, Status=%v, UpdatedAt=%v\n",
				i+1, v["id"], v["version"], v["status"], formatTime(v["updatedAt"]))
		}
	}

	// Example 3: Create a new job
	fmt.Println("\nCreating a new job...")
	newJob := map[string]interface{}{
		"id":            fmt.Sprintf("job_example_%d", time.Now().Unix()),
		"title":         "Software Engineer - Example",
		"rate":          75.0,
		"status":        "active",
		"company_id":    "comp_cab5i8o0rvh5arskod",
		"contractor_id": "cont_e0nhseq682vkoc4d",
	}
	createdJob, err := c.Update(ctx, client.EntityTypeJob, newJob)
	if err != nil {
		fmt.Printf("Error creating job: %v\n", err)
	} else {
		fmt.Printf("New job created: ID=%s, Version=%v, UID=%v\n",
			createdJob["id"], createdJob["version"], createdJob["uid"])

		// Store the job ID for use in subsequent examples
		jobID = createdJob["id"].(string)
	}

	// Example 4: Update the newly created job using safe update pattern
	fmt.Println("\nUpdating the new job's rate using safe update pattern...")

	// Define only the fields we want to update
	jobUpdate := map[string]interface{}{
		"id":   jobID,
		"rate": 80.0, // Increase the rate
	}

	// Get the latest version before updating
	result, err := UpdateEntitySafely(ctx, c, client.EntityTypeJob, jobUpdate)
	if err != nil {
		fmt.Printf("Error updating job: %v\n", err)
	} else {
		fmt.Printf("Job rate updated: ID=%s, Version=%v, Rate=%v\n",
			result["id"], result["version"], result["rate"])
	}

	// Example 5: Query active jobs for a company
	fmt.Println("\nQuerying active jobs for a company...")
	companyID := "comp_cab5i8o0rvh5arskod"
	conditions := map[string]interface{}{
		"status":     "active",
		"company_id": companyID,
	}
	options := client.QueryOptions{
		LatestVersionOnly: true,
		Limit:             10,
		SortBy:            "createdAt",
		SortDirection:     "desc",
	}
	activeJobs, err := c.Query(ctx, client.EntityTypeJob, conditions, options)
	if err != nil {
		fmt.Printf("Error querying jobs: %v\n", err)
	} else {
		fmt.Printf("Found %d active jobs for company %s:\n", len(activeJobs), companyID)
		for i, j := range activeJobs {
			fmt.Printf("  Job %d: ID=%s, Title=%v, Rate=%v\n",
				i+1, j["id"], j["title"], j["rate"])
		}
	}

	// Example 6: Batch get jobs including our new job
	fmt.Println("\nPerforming batch get operation...")
	jobIDs := []string{
		jobID,                       // Our newly created job
		"job_eysl9r8bhyis7y3lgso00", // Another job
	}
	batchResult, err := c.BatchGet(ctx, client.EntityTypeJob, jobIDs)
	if err != nil {
		fmt.Printf("Error in batch get: %v\n", err)
	} else {
		fmt.Printf("Batch get result has %d items:\n", len(batchResult))
		for id, data := range batchResult {
			fmt.Printf("  Job %s: Version=%v, Status=%v, Title=%v, Rate=%v\n",
				id, data["version"], data["status"], data["title"], data["rate"])
		}
	}

	// Example 7: Batch update jobs using safe update pattern
	fmt.Println("\nPerforming batch update operation using safe update pattern...")
	jobsToUpdate := []map[string]interface{}{
		{
			"id":     jobID,
			"status": "completed",
		},
	}
	batchUpdateResult, err := BatchUpdateEntitiesSafely(ctx, c, client.EntityTypeJob, jobsToUpdate)
	if err != nil {
		fmt.Printf("Error in batch update: %v\n", err)
	} else {
		fmt.Printf("Batch update completed for %d jobs:\n", len(batchUpdateResult))
		for id, data := range batchUpdateResult {
			fmt.Printf("  Job %s: New Version=%v, Status=%v\n",
				id, data["version"], data["status"])
		}
	}

	fmt.Println("\nAll gRPC operations completed successfully!")
}

// Helper function to format timestamps
func formatTime(timestamp interface{}) string {
	if ts, ok := timestamp.(int64); ok {
		return time.Unix(ts, 0).Format(time.RFC3339)
	}
	return fmt.Sprintf("%v", timestamp)
}

func demoGormJobOperations(db *scdGorm.DB) {
	// Access the underlying SCD client
	scdClient := db.Client()
	ctx := context.Background()

	// Example 1: Create a new job
	fmt.Println("Creating a new job...")
	// Generate a unique ID for the job to avoid duplicate key errors
	jobID := fmt.Sprintf("job_%s", generateUniqueID())

	// Create job directly with the client instead of GORM
	newJob := map[string]interface{}{
		"id":            jobID,
		"version":       1,
		"uid":           fmt.Sprintf("job_uid_%s", generateUniqueID()),
		"status":        "active",
		"rate":          27.5,
		"title":         "Full Stack Developer",
		"company_id":    "comp_cab5i8o0rvh5arskod",
		"contractor_id": "cont_e0nhseq682vkoc4d",
	}

	createdJob, err := scdClient.Update(ctx, client.EntityTypeJob, newJob)
	if err != nil {
		fmt.Printf("Error creating job: %v\n", err)
	} else {
		fmt.Printf("Job created: ID=%s, Version=%v, UID=%v\n",
			createdJob["id"], createdJob["version"], createdJob["uid"])
	}

	// Example 2: Find active jobs for a company
	fmt.Println("\nFinding active jobs for a company...")
	companyID := "comp_cab5i8o0rvh5arskod"

	// Use direct gRPC call to Query
	conditions := map[string]interface{}{
		"company_id": companyID,
		"status":     "active",
	}
	options := client.QueryOptions{
		LatestVersionOnly: true,
		SortBy:            "created_at",
		SortDirection:     "desc",
	}

	activeJobs, err := scdClient.Query(ctx, client.EntityTypeJob, conditions, options)
	if err != nil {
		fmt.Printf("Error finding jobs: %v\n", err)
	} else {
		fmt.Printf("Found %d active jobs for company %s:\n", len(activeJobs), companyID)
		for i, job := range activeJobs {
			rateVal := 0.0
			if rate, ok := job["rate"].(float64); ok {
				rateVal = rate
			}

			fmt.Printf("  Job %d: ID=%s, Title=%s, Rate=%.2f\n",
				i+1, job["id"], job["title"], rateVal)
		}
	}

	// Example 3: Find active jobs for a contractor
	fmt.Println("\nFinding active jobs for a contractor...")
	contractorID := "cont_e0nhseq682vkoc4d"

	// Use direct gRPC call to Query
	contractorConditions := map[string]interface{}{
		"contractor_id": contractorID,
		"status":        "active",
	}

	contractorJobs, err := scdClient.Query(ctx, client.EntityTypeJob, contractorConditions, options)
	if err != nil {
		fmt.Printf("Error finding jobs: %v\n", err)
	} else {
		fmt.Printf("Found %d active jobs for contractor %s:\n", len(contractorJobs), contractorID)
		for i, job := range contractorJobs {
			fmt.Printf("  Job %d: ID=%s, Title=%s, Company=%s\n",
				i+1, job["id"], job["title"], job["company_id"])
		}
	}

	// Example 4: Update a job's status
	if len(activeJobs) > 0 {
		fmt.Println("\nUpdating job status...")
		job := activeJobs[0]
		jobID := job["id"].(string)
		currentVersion := int32(1)
		if v, ok := job["version"].(int32); ok {
			currentVersion = v
		}

		// Update using the Update gRPC method
		updatedJob := map[string]interface{}{
			"id":      jobID,
			"version": currentVersion,
			"status":  "completed",
		}

		result, err := scdClient.Update(ctx, client.EntityTypeJob, updatedJob)
		if err != nil {
			fmt.Printf("Error updating job: %v\n", err)
		} else {
			fmt.Printf("Job status updated: ID=%s, Status=%s, Version=%v\n",
				result["id"], result["status"], result["version"])
		}
	}

	// Example 5: Update a job's rate
	if len(activeJobs) > 0 {
		fmt.Println("\nUpdating job rate...")
		job := activeJobs[0]
		jobID := job["id"].(string)
		currentVersion := int32(1)
		if v, ok := job["version"].(int32); ok {
			currentVersion = v
		}

		currentRate := 0.0
		if rate, ok := job["rate"].(float64); ok {
			currentRate = rate
		}

		newRate := currentRate * 1.10 // 10% increase

		// Update using the Update gRPC method
		updatedJob := map[string]interface{}{
			"id":      jobID,
			"version": currentVersion,
			"rate":    newRate,
		}

		result, err := scdClient.Update(ctx, client.EntityTypeJob, updatedJob)
		if err != nil {
			fmt.Printf("Error updating job: %v\n", err)
		} else {
			rateVal := 0.0
			if rate, ok := result["rate"].(float64); ok {
				rateVal = rate
			}

			fmt.Printf("Job rate updated: ID=%s, Rate=%.2f, Version=%v\n",
				result["id"], rateVal, result["version"])
		}
	}

	// Example 6: Find jobs with rate above threshold
	fmt.Println("\nFinding jobs with rate above 25.0...")

	// Use direct gRPC call to Query
	rateConditions := map[string]interface{}{
		"rate >": 25.0,
	}

	highRateJobs, err := scdClient.Query(ctx, client.EntityTypeJob, rateConditions, options)
	if err != nil {
		fmt.Printf("Error finding jobs: %v\n", err)
	} else {
		fmt.Printf("Found %d jobs with rate above 25.0:\n", len(highRateJobs))
		for i, job := range highRateJobs {
			rateVal := 0.0
			if rate, ok := job["rate"].(float64); ok {
				rateVal = rate
			}

			fmt.Printf("  Job %d: ID=%s, Title=%s, Rate=%.2f\n",
				i+1, job["id"], job["title"], rateVal)
		}
	}

	// Example 7: Version history for a specific job
	if len(activeJobs) > 0 {
		fmt.Println("\nQuerying version history for a job...")
		jobID := activeJobs[0]["id"].(string)

		// Use direct gRPC call to GetVersionHistory
		versions, err := scdClient.GetVersionHistory(ctx, client.EntityTypeJob, jobID)
		if err != nil {
			fmt.Printf("Error finding job versions: %v\n", err)
		} else {
			fmt.Printf("Found %d versions for job %s:\n", len(versions), jobID)
			for i, v := range versions {
				rateVal := 0.0
				if rate, ok := v["rate"].(float64); ok {
					rateVal = rate
				}

				fmt.Printf("  Version %d: UID=%s, Version=%v, Status=%s, Rate=%.2f\n",
					i+1, v["uid"], v["version"], v["status"], rateVal)
			}
		}
	}

	// Example 8: Soft delete a job
	if len(activeJobs) > 0 {
		fmt.Println("\nSoft deleting job...")
		job := activeJobs[len(activeJobs)-1] // Use the last job to avoid deleting the one we updated
		jobID := job["id"].(string)
		currentVersion := int32(1)
		if v, ok := job["version"].(int32); ok {
			currentVersion = v
		}

		// Update the status to "deleted" using the Update gRPC method
		deletedJob := map[string]interface{}{
			"id":      jobID,
			"version": currentVersion,
			"status":  "deleted",
		}

		result, err := scdClient.Update(ctx, client.EntityTypeJob, deletedJob)
		if err != nil {
			fmt.Printf("Error deleting job: %v\n", err)
		} else {
			fmt.Printf("Job marked as deleted: ID=%s, New Status: %s\n",
				result["id"], result["status"])

			// Verify the job is now marked as deleted
			getResult, err := scdClient.GetLatestVersion(ctx, client.EntityTypeJob, jobID)
			if err != nil {
				fmt.Printf("Error checking deleted job: %v\n", err)
			} else {
				fmt.Printf("Job status after deletion: %s\n", getResult["status"])
			}
		}
	}
}
