package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/mercor-ai/go-sdc-client-library/pkg/client"
)

// RunSafeUpdateExample demonstrates the complete flow of fetching and updating entities safely
func RunSafeUpdateExample() {
	fmt.Println("\n===== SAFE UPDATE PATTERN COMPREHENSIVE EXAMPLE =====")
	fmt.Println("This example demonstrates the safe update pattern using known job entities:")
	fmt.Println("1. Working with specified job IDs in the database")
	fmt.Println("2. Performing Get operations (single and batch)")
	fmt.Println("3. Fetching latest versions")
	fmt.Println("4. Using latest versions to perform update operations")
	fmt.Println("5. Batch get to verify updated data")

	// Get environment variables for configuration
	scdHost := getEnvForSafeExample("SCD_HOST", "localhost")
	scdPort := 9090 // Default SCD service port

	// Initialize the SCD client
	scdClient, err := client.NewClientWithHostPort(scdHost, scdPort)
	if err != nil {
		log.Fatalf("Failed to create SCD client: %v", err)
	}
	defer scdClient.Close()

	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	// Step 1: Work with known job IDs
	fmt.Println("\n=== STEP 1: Working with Known Job IDs ===")

	// Known job IDs from the provided data
	jobIDs := []string{
		"job_ckbk6oo4hn7pacdgcz9f",  // Software Engineer, active, rate 15.50
		"job_eysl9r8bhyis7y3lgso00", // ML Engineer, active, rate 32.50
		"job_pwq7nl3eky5u2t9oxzm",   // UI Designer, active, rate 25.00
		"job_td24kl7cpq9mnr8vzx",    // Data Engineer, completed, rate 28.00
	}

	fmt.Println("Working with the following known job IDs:")
	for i, jobID := range jobIDs {
		fmt.Printf("  %d. %s\n", i+1, jobID)
	}

	// Verify these jobs exist in the database
	verifyJobsExist(ctx, scdClient, jobIDs)

	// Steps 2-5: Use the gRPC client for all operations
	performClientOperations(ctx, scdClient, jobIDs)
}

// Helper function to get environment variable with fallback
func getEnvForSafeExample(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}

// Verify jobs exist in the database
func verifyJobsExist(ctx context.Context, c *client.Client, jobIDs []string) {
	fmt.Println("\nVerifying jobs exist in the database...")

	// Batch get the jobs
	batchResult, err := c.BatchGet(ctx, client.EntityTypeJob, jobIDs)
	if err != nil {
		fmt.Printf("Error in batch get: %v\n", err)
		return
	}

	fmt.Printf("Successfully found %d jobs in the database:\n", len(batchResult))
	for id, data := range batchResult {
		status := data["status"]
		title := data["title"]
		rate := data["rate"]
		version := data["version"]

		fmt.Printf("  • Job %s: Title=%v, Status=%v, Rate=%v, Version=%v\n",
			id, title, status, rate, version)
	}

	// Check if any job IDs were not found
	for _, jobID := range jobIDs {
		if _, exists := batchResult[jobID]; !exists {
			fmt.Printf("Warning: Job %s not found in the database\n", jobID)
		}
	}
}

// Perform all client operations using the gRPC client with retries
func performClientOperations(ctx context.Context, c *client.Client, jobIDs []string) {
	if len(jobIDs) == 0 {
		fmt.Println("No jobs to work with. Exiting...")
		return
	}

	// Step 2: Perform Get operations (single and batch)
	fmt.Println("\n=== STEP 2: Performing Get Operations (Single and Batch) ===")

	// Single get operation for a specific job (Software Engineer)
	softwareEngineerID := jobIDs[0] // job_ckbk6oo4hn7pacdgcz9f
	fmt.Printf("Getting Software Engineer job (ID: %s)...\n", softwareEngineerID)
	var swEngJob map[string]interface{}
	var err error

	// Get latest version
	swEngJob, err = c.GetLatestVersion(ctx, client.EntityTypeJob, softwareEngineerID)
	if err != nil {
		fmt.Printf("Error getting Software Engineer job: %v\n", err)
	} else {
		fmt.Printf("Retrieved Software Engineer job: ID=%s, Version=%v, Title=%v, Rate=%v, Status=%v\n",
			swEngJob["id"], swEngJob["version"], swEngJob["title"], swEngJob["rate"], swEngJob["status"])
	}

	// Get history of Data Engineer job
	dataEngineerID := jobIDs[3] // job_td24kl7cpq9mnr8vzx
	fmt.Printf("\nGetting version history for Data Engineer job (ID: %s)...\n", dataEngineerID)
	history, err := c.GetVersionHistory(ctx, client.EntityTypeJob, dataEngineerID)
	if err != nil {
		fmt.Printf("Error getting job history: %v\n", err)
	} else {
		fmt.Printf("Found %d versions for Data Engineer job:\n", len(history))
		for i, version := range history {
			fmt.Printf("  Version %d: Version=%v, Status=%v, Rate=%v, UpdatedAt=%v\n",
				i+1, version["version"], version["status"], version["rate"], version["updated_at"])
		}
	}

	// Batch get operation for all jobs
	fmt.Println("\nPerforming batch get operation for all jobs...")
	var batchResult map[string]map[string]interface{}

	batchResult, err = c.BatchGet(ctx, client.EntityTypeJob, jobIDs)
	if err != nil {
		fmt.Printf("Error in batch get: %v\n", err)
	} else {
		fmt.Printf("Batch get result has %d items:\n", len(batchResult))
		for id, data := range batchResult {
			fmt.Printf("  Job %s: Version=%v, Title=%v, Rate=%v, Status=%v\n",
				id, data["version"], data["title"], data["rate"], data["status"])
		}
	}

	// Step 3: Fetch latest versions explicitly (demonstrating the pattern)
	fmt.Println("\n=== STEP 3: Fetching Latest Versions for Update ===")
	latestVersions := make(map[string]map[string]interface{})

	fmt.Println("Fetching latest versions for all jobs...")
	for _, jobID := range jobIDs {
		latestJob, err := c.GetLatestVersion(ctx, client.EntityTypeJob, jobID)
		if err != nil {
			fmt.Printf("Error getting latest version for job %s: %v\n", jobID, err)
			continue
		}

		latestVersions[jobID] = latestJob
		fmt.Printf("Latest version for job %s: Version=%v, Status=%v\n",
			jobID, latestJob["version"], latestJob["status"])

		// Add a small delay between fetches
		time.Sleep(100 * time.Millisecond)
	}

	// Step 4: Use latest versions to perform update operations
	fmt.Println("\n=== STEP 4: Performing Update Operations Using Latest Versions ===")

	// Update 1: Increase rate for Software Engineer by 10%
	if swEngVersion, ok := latestVersions[softwareEngineerID]; ok {
		fmt.Printf("Updating Software Engineer job rate...\n")

		// Get current rate
		currentRate := 0.0
		if rate, ok := swEngVersion["rate"].(float64); ok {
			currentRate = rate
		}

		// Create update data with correct version
		updateData := map[string]interface{}{
			"id":      softwareEngineerID,
			"version": swEngVersion["version"],
			"rate":    currentRate * 1.10, // 10% increase
		}

		// Update the job
		updatedJob, err := c.Update(ctx, client.EntityTypeJob, updateData)
		if err != nil {
			fmt.Printf("Error updating Software Engineer job: %v\n", err)
		} else {
			fmt.Printf("Software Engineer job updated: ID=%s, New Version=%v, New Rate=%v\n",
				updatedJob["id"], updatedJob["version"], updatedJob["rate"])

			// Save the updated version for later verification
			latestVersions[softwareEngineerID] = updatedJob
		}

		// Add a small delay before next operation
		time.Sleep(500 * time.Millisecond)
	}

	// Update 2: Change status of ML Engineer job to "extended"
	mlEngineerID := jobIDs[1] // job_eysl9r8bhyis7y3lgso00
	if mlEngVersion, ok := latestVersions[mlEngineerID]; ok {
		fmt.Printf("\nUpdating ML Engineer job status to extended...\n")

		// Create update data with correct version
		updateData := map[string]interface{}{
			"id":      mlEngineerID,
			"version": mlEngVersion["version"],
			"status":  "extended",
		}

		// Update the job
		updatedJob, err := c.Update(ctx, client.EntityTypeJob, updateData)
		if err != nil {
			fmt.Printf("Error updating ML Engineer job: %v\n", err)
		} else {
			fmt.Printf("ML Engineer job updated: ID=%s, New Version=%v, New Status=%v\n",
				updatedJob["id"], updatedJob["version"], updatedJob["status"])

			// Save the updated version for later verification
			latestVersions[mlEngineerID] = updatedJob
		}

		// Add a small delay before next operation
		time.Sleep(500 * time.Millisecond)
	}

	// Update 3: Batch update the remaining jobs
	var jobsToUpdate []interface{}

	// Update UI Designer job title and Data Engineer job title
	uiDesignerID := jobIDs[2]  // job_pwq7nl3eky5u2t9oxzm
	dataEngineerID = jobIDs[3] // job_td24kl7cpq9mnr8vzx

	// Only include jobs that we found latest versions for
	if uiVersion, ok := latestVersions[uiDesignerID]; ok {
		// Get current title
		currentTitle := ""
		if title, ok := uiVersion["title"].(string); ok {
			currentTitle = title
		}

		jobsToUpdate = append(jobsToUpdate, map[string]interface{}{
			"id":      uiDesignerID,
			"version": uiVersion["version"],
			"title":   fmt.Sprintf("%s - Senior", currentTitle),
		})
	}

	if deVersion, ok := latestVersions[dataEngineerID]; ok {
		// Check current status - only update if it's not already completed
		currentStatus := ""
		if status, ok := deVersion["status"].(string); ok {
			currentStatus = status
		}

		// Only update if not already completed
		if currentStatus != "completed" {
			jobsToUpdate = append(jobsToUpdate, map[string]interface{}{
				"id":      dataEngineerID,
				"version": deVersion["version"],
				"status":  "completed",
			})
		} else {
			fmt.Printf("Data Engineer job is already completed. Skipping update.\n")
		}
	}

	// Perform batch update if we have jobs to update
	if len(jobsToUpdate) > 0 {
		fmt.Printf("\nPerforming batch update for %d remaining jobs...\n", len(jobsToUpdate))

		batchUpdateResult, err := c.BatchUpdate(ctx, client.EntityTypeJob, jobsToUpdate)
		if err != nil {
			fmt.Printf("Error in batch update: %v\n", err)
		} else {
			fmt.Printf("Batch update completed for %d jobs:\n", len(batchUpdateResult))
			for id, data := range batchUpdateResult {
				fmt.Printf("  Job %s: New Version=%v, Title=%v, Status=%v\n",
					id, data["version"], data["title"], data["status"])

				// Save the updated versions for verification
				latestVersions[id] = data
			}
		}
	} else {
		fmt.Println("\nNo jobs to update in batch.")
	}

	// Step 5: Batch get to verify updated data
	fmt.Println("\n=== STEP 5: Verifying Updates with Batch Get ===")

	// Add a delay to ensure all updates are processed
	time.Sleep(1 * time.Second)

	verifyBatchResult, err := c.BatchGet(ctx, client.EntityTypeJob, jobIDs)
	if err != nil {
		fmt.Printf("Error in verification batch get: %v\n", err)
	} else {
		fmt.Printf("Retrieved %d jobs after updates:\n", len(verifyBatchResult))

		// Create a map to store the original values
		originalValues := make(map[string]map[string]interface{})
		for id, job := range batchResult {
			originalValues[id] = map[string]interface{}{
				"version": job["version"],
				"status":  job["status"],
				"title":   job["title"],
				"rate":    job["rate"],
			}
		}

		// Compare and display changes
		for id, data := range verifyBatchResult {
			fmt.Printf("  Job %s: Version=%v, Status=%v, Title=%v, Rate=%v\n",
				id, data["version"], data["status"], data["title"], data["rate"])

			// Compare with original data
			if original, exists := originalValues[id]; exists {
				// Check what changed
				changes := []string{}

				// Check version change
				if original["version"] != data["version"] {
					changes = append(changes, fmt.Sprintf("Version: %v → %v",
						original["version"], data["version"]))
				}

				// Check status change
				if original["status"] != data["status"] {
					changes = append(changes, fmt.Sprintf("Status: %v → %v",
						original["status"], data["status"]))
				}

				// Check title change
				if original["title"] != data["title"] {
					changes = append(changes, fmt.Sprintf("Title: %v → %v",
						original["title"], data["title"]))
				}

				// Check rate change
				if original["rate"] != data["rate"] {
					changes = append(changes, fmt.Sprintf("Rate: %v → %v",
						original["rate"], data["rate"]))
				}

				// Display changes
				if len(changes) > 0 {
					fmt.Println("    Changes detected:")
					for _, change := range changes {
						fmt.Printf("    ✓ %s\n", change)
					}
				} else {
					fmt.Println("    No changes detected")
				}
			}
		}
	}

	fmt.Println("\nSafe update example completed!")
}
