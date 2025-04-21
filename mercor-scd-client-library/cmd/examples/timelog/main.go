// cmd/examples/timelog/main.go
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
	timelogID := flag.String("id", "", "Timelog ID to query (required)")
	adjustDuration := flag.Int64("adjust", 0, "Absolute duration value to set (in milliseconds, optional)")
	flag.Parse()

	// Validate required parameters
	if *timelogID == "" {
		flag.PrintDefaults()
		fmt.Println("\nError: Timelog ID is required")
		os.Exit(1)
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

	// Create timelog repository
	timelogRepo := repository.NewTimelogRepository(db, scdClient)

	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	// Example 1: Get the latest version of the existing timelog
	fmt.Printf("Getting latest version of timelog with ID %s...\n", *timelogID)

	existingTimelog, err := timelogRepo.GetTimelogRemote(ctx, *timelogID)
	if err != nil {
		log.Fatalf("Failed to get timelog from SCD service: %v", err)
	}

	fmt.Printf("Retrieved timelog: ID=%s, Version=%d, UID=%s, Duration=%d, Type=%s\n",
		existingTimelog.ID, existingTimelog.Version, existingTimelog.UID,
		existingTimelog.Duration, existingTimelog.Type)

	// Example 2: Adjust timelog duration through SCD service if an adjustment was specified
	if *adjustDuration != 0 {
		fmt.Println("\nAdjusting timelog duration through SCD service...")

		// Use the specified absolute duration value
		adjustedDuration := *adjustDuration

		fmt.Printf("Adjusting duration from %d to %d ms...\n",
			existingTimelog.Duration, adjustedDuration)

		adjustedTimelog, err := timelogRepo.AdjustTimelogRemote(ctx, *timelogID, adjustedDuration)
		if err != nil {
			log.Fatalf("Failed to adjust timelog through SCD service: %v", err)
		}

		fmt.Printf("Adjusted timelog: ID=%s, Version=%d, UID=%s, Duration=%d (adjusted from %d)\n",
			adjustedTimelog.ID, adjustedTimelog.Version, adjustedTimelog.UID,
			adjustedTimelog.Duration, existingTimelog.Duration)
	} else {
		fmt.Println("\nNo duration adjustment specified, skipping adjustment step.")
	}

	// Example 3: Get timelogs for the job associated with this timelog
	fmt.Println("\nGetting timelogs for the associated job...")
	jobTimelogs, err := timelogRepo.GetTimelogsForJobRemote(ctx, existingTimelog.JobUID)
	if err != nil {
		log.Fatalf("Failed to get job timelogs: %v", err)
	}

	fmt.Printf("Found %d timelogs for job %s:\n", len(jobTimelogs), existingTimelog.JobUID)
	for _, tl := range jobTimelogs {
		fmt.Printf("- ID=%s, Version=%d, UID=%s, Duration=%d, Type=%s\n",
			tl.ID, tl.Version, tl.UID, tl.Duration, tl.Type)
	}

	// Example 4: Get timelog version history from SCD service
	fmt.Println("\nGetting timelog history from SCD service...")
	timelogHistory, err := timelogRepo.GetTimelogHistoryRemote(ctx, *timelogID)
	if err != nil {
		log.Fatalf("Failed to get timelog history: %v", err)
	}

	fmt.Printf("Timelog history for %s has %d versions:\n", *timelogID, len(timelogHistory))
	for _, version := range timelogHistory {
		fmt.Printf("- Version=%d, UID=%s, Type=%s, Duration=%d, Created=%s\n",
			version.Version, version.UID, version.Type, version.Duration,
			version.CreatedAt.Format(time.RFC3339))
	}
}
