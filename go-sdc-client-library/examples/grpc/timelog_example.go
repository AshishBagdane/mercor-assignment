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

// TimelogEntity represents a timelog entity
type TimelogEntity struct {
	model.SCDModel        // Embed SCD fields
	Duration       int64  `json:"duration"`   // BigInt (in milliseconds)
	TimeStart      int64  `json:"time_start"` // BigInt (Timestamp in milliseconds)
	TimeEnd        int64  `json:"time_end"`   // BigInt (Timestamp in milliseconds)
	Type           string `json:"type"`       // Enum: "captured", "adjusted"
	JobUID         string `json:"job_uid"`    // Foreign Key
}

// TableName specifies the table name for TimelogEntity
func (TimelogEntity) TableName() string {
	return "timelogs"
}

// EntityType returns the SCD entity type
func (TimelogEntity) EntityType() string {
	return "timelog"
}

// Helper for displaying time duration in a human-readable format
func formatDuration(durationMs int64) string {
	hours := float64(durationMs) / 3600000.0 // Convert milliseconds to hours
	return fmt.Sprintf("%.1f hours", hours)
}

// Helper for displaying time in a human-readable format
func formatTimeMs(timeMs int64) string {
	return time.UnixMilli(timeMs).Format("2006-01-02 15:04:05")
}

// RunTimelogExamples is the exported function that will be called from main.go
func RunTimelogExamples() {
	// Get environment variables for configuration
	scdHost := getEnvForTimelog("SCD_HOST", "localhost")
	scdPort := 9090 // Default SCD service port
	dbHost := getEnvForTimelog("DB_HOST", "localhost")
	dbPort := getEnvForTimelog("DB_PORT", "5432")
	dbUser := getEnvForTimelog("DB_USER", "user")
	dbPass := getEnvForTimelog("DB_PASSWORD", "password")
	dbName := getEnvForTimelog("DB_NAME", "employment")
	sslMode := getEnvForTimelog("DB_SSL_MODE", "disable")

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

	fmt.Println("\n===== TIMELOG OPERATIONS EXAMPLE =====")

	// Create context with timeout
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	// Direct Client API Examples
	fmt.Println("\n=== DIRECT CLIENT API EXAMPLES ===")
	demoDirectTimelogOperations(ctx, scdClient)

	// GORM Integration Examples
	fmt.Println("\n=== GORM INTEGRATION EXAMPLES ===")
	demoGormTimelogOperations(db)
}

// Helper function to get environment variable with fallback
func getEnvForTimelog(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}

func demoDirectTimelogOperations(ctx context.Context, c *client.Client) {
	// Example 1: Get latest version of a timelog
	timelogID := "tl_AAABk__7Gd2t3TqM-Bdm8kNQ"
	fmt.Printf("Getting latest version of timelog %s...\n", timelogID)
	timelogData, err := c.GetLatestVersion(ctx, "timelog", timelogID)
	if err != nil {
		fmt.Printf("Error getting timelog: %v\n", err)
	} else {
		fmt.Printf("Latest timelog version: ID=%s, Version=%v\n",
			timelogData["id"], timelogData["version"])

		if duration, ok := timelogData["duration"].(int64); ok {
			fmt.Printf("  Duration: %s\n", formatDuration(duration))
		}

		if timeStart, ok := timelogData["time_start"].(int64); ok {
			fmt.Printf("  Start Time: %s\n", formatTimeMs(timeStart))
		}

		if timeEnd, ok := timelogData["time_end"].(int64); ok {
			fmt.Printf("  End Time: %s\n", formatTimeMs(timeEnd))
		}
	}

	// Example 2: Get version history of a timelog
	fmt.Printf("\nGetting version history of timelog %s...\n", timelogID)
	timelogVersions, err := c.GetVersionHistory(ctx, "timelog", timelogID)
	if err != nil {
		fmt.Printf("Error getting timelog history: %v\n", err)
	} else {
		fmt.Printf("Found %d versions:\n", len(timelogVersions))
		for i, v := range timelogVersions {
			duration := int64(0)
			if d, ok := v["duration"].(int64); ok {
				duration = d
			}

			fmt.Printf("  Version %d: ID=%s, Version=%v, Type=%v, Duration=%s\n",
				i+1, v["id"], v["version"], v["type"], formatDuration(duration))
		}
	}

	// Example 3: Query timelogs for a job
	fmt.Println("\nQuerying timelogs for a job...")
	jobUID := "job_uid_ywij5sh1tvfp5nkq7azav"
	conditions := map[string]interface{}{
		"job_uid": jobUID,
	}
	options := client.QueryOptions{
		LatestVersionOnly: true,
		SortBy:            "time_start",
		SortDirection:     "desc",
	}
	timelogs, err := c.Query(ctx, "timelog", conditions, options)
	if err != nil {
		fmt.Printf("Error querying timelogs: %v\n", err)
	} else {
		fmt.Printf("Found %d timelogs for job %s:\n", len(timelogs), jobUID)
		totalDuration := int64(0)
		for i, t := range timelogs {
			var duration int64 = 0
			if d, ok := t["duration"].(int64); ok {
				duration = d
				totalDuration += duration
			}

			fmt.Printf("  Timelog %d: ID=%s, Duration=%s, Type=%v\n",
				i+1, t["id"], formatDuration(duration), t["type"])
		}
		fmt.Printf("  Total Duration: %s\n", formatDuration(totalDuration))
	}

	// Example 4: Query timelogs for a time period
	fmt.Println("\nQuerying timelogs for March 2023...")
	startTime := time.Date(2023, 3, 1, 0, 0, 0, 0, time.UTC).UnixMilli()
	endTime := time.Date(2023, 3, 31, 23, 59, 59, 999999999, time.UTC).UnixMilli()

	conditions = map[string]interface{}{
		"time_start >=": startTime,
		"time_end <=":   endTime,
	}
	timelogsPeriod, err := c.Query(ctx, "timelog", conditions, options)
	if err != nil {
		fmt.Printf("Error querying timelogs: %v\n", err)
	} else {
		fmt.Printf("Found %d timelogs between %s and %s:\n",
			len(timelogsPeriod),
			formatTimeMs(startTime),
			formatTimeMs(endTime))

		for i, t := range timelogsPeriod {
			var duration int64 = 0
			if d, ok := t["duration"].(int64); ok {
				duration = d
			}

			fmt.Printf("  Timelog %d: ID=%s, Duration=%s\n",
				i+1, t["id"], formatDuration(duration))
		}
	}

	// Example 5: Adjust a timelog
	fmt.Println("\nAdjusting timelog duration...")

	// Create an adjusted version using the safe update pattern
	updatedTimelog := map[string]interface{}{
		"id":       timelogID,
		"duration": int64(1800000), // 30 minutes in milliseconds
		"type":     "adjusted",
	}
	result, err := UpdateEntitySafely(ctx, c, "timelog", updatedTimelog)
	if err != nil {
		fmt.Printf("Error updating timelog: %v\n", err)
	} else {
		var duration int64 = 0
		if d, ok := result["duration"].(int64); ok {
			duration = d
		}

		fmt.Printf("Timelog adjusted: ID=%s, Version=%v, New Duration=%s, Type=%v\n",
			result["id"], result["version"], formatDuration(duration), result["type"])
	}

	// Example 6: Query timelogs with duration above threshold
	fmt.Println("\nQuerying timelogs with duration above 1 hour...")
	threshold := int64(3600000) // 1 hour in milliseconds
	conditions = map[string]interface{}{
		"duration >": threshold,
	}
	longTimelogs, err := c.Query(ctx, "timelog", conditions, options)
	if err != nil {
		fmt.Printf("Error querying timelogs: %v\n", err)
	} else {
		fmt.Printf("Found %d timelogs with duration above %s:\n",
			len(longTimelogs), formatDuration(threshold))

		for i, t := range longTimelogs {
			var duration int64 = 0
			if d, ok := t["duration"].(int64); ok {
				duration = d
			}

			fmt.Printf("  Timelog %d: ID=%s, Duration=%s\n",
				i+1, t["id"], formatDuration(duration))
		}
	}

	// Example 7: Batch get timelogs
	fmt.Println("\nPerforming batch get operation...")
	timelogIDs := []string{
		"tl_AAABk__7Gd2t3TqM-Bdm8kNQ",
		"tl_AAABk__7Gj7qDYLhxS0UWehe",
	}
	batchResult, err := c.BatchGet(ctx, "timelog", timelogIDs)
	if err != nil {
		fmt.Printf("Error in batch get: %v\n", err)
	} else {
		fmt.Printf("Batch get result has %d items:\n", len(batchResult))
		for id, data := range batchResult {
			var duration int64 = 0
			if d, ok := data["duration"].(int64); ok {
				duration = d
			}

			fmt.Printf("  Timelog %s: Version=%v, Duration=%s, Type=%v\n",
				id, data["version"], formatDuration(duration), data["type"])
		}
	}
}

func demoGormTimelogOperations(db *scdGorm.DB) {
	// Access the underlying SCD client
	scdClient := db.Client()
	ctx := context.Background()

	// Example 1: Create a new timelog
	fmt.Println("Creating a new timelog...")
	// First find a job to associate this timelog with
	var jobUID string = "job_uid_ywij5sh1tvfp5nkq7azav" // Default if we can't find one

	// Find a job from the DB - use Query API instead of direct DB access
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

	// Create timelog with unique ID and UID
	timelogID := fmt.Sprintf("tl_%s", generateUniqueID())
	now := time.Now().UnixMilli()
	twoHours := int64(7200000) // 2 hours in milliseconds
	newTimelog := TimelogEntity{
		SCDModel: model.SCDModel{
			ID:      timelogID,
			Version: 1,
			UID:     fmt.Sprintf("tl_uid_%s", generateUniqueID()),
		},
		Duration:  twoHours,
		TimeStart: now - twoHours,
		TimeEnd:   now,
		Type:      "captured",
		JobUID:    jobUID,
	}

	if result := db.Create(&newTimelog); result.Error != nil {
		fmt.Printf("Error creating timelog: %v\n", result.Error)
	} else {
		fmt.Printf("Timelog created: ID=%s, Version=%d, UID=%s\n",
			newTimelog.ID, newTimelog.Version, newTimelog.UID)
		fmt.Printf("  Duration: %s (%s to %s)\n",
			formatDuration(newTimelog.Duration),
			formatTimeMs(newTimelog.TimeStart),
			formatTimeMs(newTimelog.TimeEnd))
	}

	// Example 2: Find timelogs for a job
	fmt.Println("\nFinding timelogs for a job...")

	// Use direct gRPC call to Query instead of SQL
	timelogConditions := map[string]interface{}{
		"job_uid": jobUID,
	}
	options := client.QueryOptions{
		LatestVersionOnly: true,
		SortBy:            "time_start",
		SortDirection:     "desc",
	}

	timelogs, err := scdClient.Query(ctx, "timelog", timelogConditions, options)
	if err != nil {
		fmt.Printf("Error finding timelogs: %v\n", err)
	} else {
		fmt.Printf("Found %d timelogs for job %s:\n", len(timelogs), jobUID)
		totalDuration := int64(0)

		for i, t := range timelogs {
			duration := int64(0)
			if d, ok := t["duration"].(int64); ok {
				duration = d
				totalDuration += duration
			}

			fmt.Printf("  Timelog %d: ID=%s, Duration=%s, Type=%s\n",
				i+1, t["id"], formatDuration(duration), t["type"])
		}
		fmt.Printf("  Total Duration: %s\n", formatDuration(totalDuration))
	}

	// Example 3: Find timelogs for a time period
	fmt.Println("\nFinding timelogs for a specific time period...")
	startTime := time.Now().Add(-30 * 24 * time.Hour).UnixMilli() // Last 30 days
	endTime := time.Now().UnixMilli()

	// Use direct gRPC call to Query with time conditions
	periodConditions := map[string]interface{}{
		"time_start >=": startTime,
		"time_end <=":   endTime,
	}

	periodTimelogs, err := scdClient.Query(ctx, "timelog", periodConditions, options)
	if err != nil {
		fmt.Printf("Error finding timelogs: %v\n", err)
	} else {
		fmt.Printf("Found %d timelogs between %s and %s:\n",
			len(periodTimelogs),
			formatTimeMs(startTime),
			formatTimeMs(endTime))

		for i, t := range periodTimelogs {
			duration := int64(0)
			if d, ok := t["duration"].(int64); ok {
				duration = d
			}

			timeStart := int64(0)
			if ts, ok := t["time_start"].(int64); ok {
				timeStart = ts
			}

			fmt.Printf("  Timelog %d: ID=%s, Duration=%s, Start=%s\n",
				i+1, t["id"], formatDuration(duration), formatTimeMs(timeStart))
		}
	}

	// Example 4: Adjust a timelog
	if len(timelogs) > 0 {
		fmt.Println("\nAdjusting timelog duration...")
		timelog := timelogs[0]
		timelogID := timelog["id"].(string)

		currentDuration := int64(0)
		if d, ok := timelog["duration"].(int64); ok {
			currentDuration = d
		}

		adjustedDuration := currentDuration * 90 / 100 // Reduce by 10%

		// Update using the safe update pattern
		updatedTimelog := map[string]interface{}{
			"id":       timelogID,
			"duration": adjustedDuration,
			"type":     "adjusted",
		}

		result, err := UpdateEntitySafely(ctx, scdClient, "timelog", updatedTimelog)
		if err != nil {
			fmt.Printf("Error adjusting timelog: %v\n", err)
		} else {
			newDuration := int64(0)
			if d, ok := result["duration"].(int64); ok {
				newDuration = d
			}

			fmt.Printf("Timelog adjusted: ID=%s, Version=%v, New Duration=%s\n",
				result["id"], result["version"], formatDuration(newDuration))
		}
	}

	// Example 5: Find timelogs with duration above threshold
	fmt.Println("\nFinding timelogs with duration above 1 hour...")
	threshold := int64(3600000) // 1 hour in milliseconds

	// Use direct gRPC call to Query with duration threshold
	thresholdConditions := map[string]interface{}{
		"duration >": threshold,
	}

	longTimelogs, err := scdClient.Query(ctx, "timelog", thresholdConditions, options)
	if err != nil {
		fmt.Printf("Error finding timelogs: %v\n", err)
	} else {
		fmt.Printf("Found %d timelogs with duration above %s:\n",
			len(longTimelogs), formatDuration(threshold))

		for i, t := range longTimelogs {
			duration := int64(0)
			if d, ok := t["duration"].(int64); ok {
				duration = d
			}

			fmt.Printf("  Timelog %d: ID=%s, Duration=%s, Type=%s\n",
				i+1, t["id"], formatDuration(duration), t["type"])
		}
	}

	// Example 6: Version history for a specific timelog
	if len(timelogs) > 0 {
		fmt.Println("\nQuerying version history for a timelog...")
		timelogID := timelogs[0]["id"].(string)

		// Use direct gRPC call to GetVersionHistory instead of SQL
		versions, err := scdClient.GetVersionHistory(ctx, "timelog", timelogID)
		if err != nil {
			fmt.Printf("Error finding timelog versions: %v\n", err)
		} else {
			fmt.Printf("Found %d versions for timelog %s:\n", len(versions), timelogID)
			for i, v := range versions {
				duration := int64(0)
				if d, ok := v["duration"].(int64); ok {
					duration = d
				}

				fmt.Printf("  Version %d: UID=%s, Version=%v, Type=%s, Duration=%s\n",
					i+1, v["uid"], v["version"], v["type"], formatDuration(duration))
			}
		}
	}
}
