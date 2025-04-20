package main

import (
	"context"
	"fmt"
	"log"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"

	"github.com/mercor-ai/scd-go-client/pkg/client"
	"github.com/mercor-ai/scd-go-client/pkg/config"
	"github.com/mercor-ai/scd-go-client/pkg/repository"
)

func main() {
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

	// Create job repository
	jobRepo := repository.NewJobRepository(db, scdClient)

	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 300*time.Second)
	defer cancel()

	// Use existing job ID
	existingJobID := "job_ckbk6oo4hn7pacdgcz9f"

	// Example 1: Get the latest version of the existing job
	fmt.Printf("Getting latest version of job with ID %s...\n", existingJobID)

	existingJob, err := jobRepo.GetJobRemote(ctx, existingJobID)
	if err != nil {
		log.Fatalf("Failed to get job from SCD service: %v", err)
	}

	fmt.Printf("Retrieved job: ID=%s, Version=%d, UID=%s, Title=%s, Status=%s, Rate=%.2f\n",
		existingJob.ID, existingJob.Version, existingJob.UID,
		existingJob.Title, existingJob.Status, existingJob.Rate)

	// Example 2: Update job status through SCD service
	fmt.Println("\nUpdating job status through SCD service...")

	// Toggle status between "active" and "extended"
	newStatus := "active"
	if existingJob.Status == "active" {
		newStatus = "extended"
	}

	updatedJob, err := jobRepo.UpdateJobStatusRemote(ctx, existingJobID, newStatus)
	if err != nil {
		log.Fatalf("Failed to update job status through SCD service: %v", err)
	}

	fmt.Printf("Updated job: ID=%s, Version=%d, UID=%s, Status=%s (changed from %s)\n",
		updatedJob.ID, updatedJob.Version, updatedJob.UID,
		updatedJob.Status, existingJob.Status)

	// Example 3: Get active jobs for the company associated with this job
	fmt.Println("\nGetting active jobs for the company...")
	activeJobs, err := jobRepo.GetActiveJobsForCompanyRemote(ctx, existingJob.CompanyID)
	if err != nil {
		log.Fatalf("Failed to get active jobs: %v", err)
	}

	fmt.Printf("Found %d active jobs for company %s:\n", len(activeJobs), existingJob.CompanyID)
	for _, job := range activeJobs {
		fmt.Printf("- ID=%s, Version=%d, UID=%s, Title=%s, Status=%s, Rate=%.2f\n",
			job.ID, job.Version, job.UID, job.Title, job.Status, job.Rate)
	}

	// Example 4: Get job version history from SCD service
	fmt.Println("\nGetting job history from SCD service...")
	jobHistory, err := jobRepo.GetJobHistoryRemote(ctx, existingJobID)
	if err != nil {
		log.Fatalf("Failed to get job history: %v", err)
	}

	fmt.Printf("Job history for %s has %d versions:\n", existingJobID, len(jobHistory))
	for _, version := range jobHistory {
		fmt.Printf("- Version=%d, UID=%s, Status=%s, Created=%s\n",
			version.Version, version.UID, version.Status,
			version.CreatedAt.Format(time.RFC3339))
	}
}
