package main

import (
	"fmt"
	"log"
	"os"
	"time"

	"gorm.io/driver/postgres"
	gormLib "gorm.io/gorm"
)

// Job represents a job entity from jobs.json
type Job struct {
	ID           string  `json:"id"`
	Version      int32   `json:"version"`
	UID          string  `json:"uid"`
	Status       string  `json:"status"`        // Enum: "extended", "active", "completed"
	Rate         float64 `json:"rate"`          // Decimal (10,2)
	Title        string  `json:"title"`         // String
	CompanyID    string  `json:"company_id"`    // Foreign Key
	ContractorID string  `json:"contractor_id"` // Foreign Key
	CreatedAt    string  `json:"created_at"`
	UpdatedAt    string  `json:"updated_at"`
}

// TableName specifies the table name for Job
func (Job) TableName() string {
	return "jobs"
}

// Timelog represents a timelog entity from timelog.json
type Timelog struct {
	ID        string `json:"id"`
	Version   int32  `json:"version"`
	UID       string `json:"uid"`
	Duration  int64  `json:"duration"`   // BigInt
	TimeStart int64  `json:"time_start"` // BigInt (Timestamp in milliseconds)
	TimeEnd   int64  `json:"time_end"`   // BigInt (Timestamp in milliseconds)
	Type      string `json:"type"`       // Enum: "captured", "adjusted"
	JobUID    string `json:"job_uid"`    // Foreign Key to jobs.uid
	CreatedAt string `json:"created_at"`
	UpdatedAt string `json:"updated_at"`
}

// TableName specifies the table name for Timelog
func (Timelog) TableName() string {
	return "timelogs"
}

// PaymentLineItem represents a payment line item entity
type PaymentLineItem struct {
	ID         string  `json:"id"`
	Version    int32   `json:"version"`
	UID        string  `json:"uid"`
	JobUID     string  `json:"job_uid"`     // Foreign Key to jobs.uid
	TimelogUID string  `json:"timelog_uid"` // Foreign Key to timelogs.uid
	Amount     float64 `json:"amount"`      // Decimal (10,2)
	Status     string  `json:"status"`      // Enum: "pending", "paid"
	CreatedAt  string  `json:"created_at"`
	UpdatedAt  string  `json:"updated_at"`
}

// TableName specifies the table name for PaymentLineItem
func (PaymentLineItem) TableName() string {
	return "payment_line_items"
}

func main() {
	// Get database connection details from environment variables
	dbHost := getEnv("DB_HOST", "localhost")
	dbPort := getEnv("DB_PORT", "5432")
	dbUser := getEnv("DB_USER", "user")
	dbPass := getEnv("DB_PASSWORD", "password")
	dbName := getEnv("DB_NAME", "employment")
	sslMode := getEnv("DB_SSL_MODE", "disable")

	// Create PostgreSQL connection string
	dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=%s TimeZone=UTC",
		dbHost, dbUser, dbPass, dbName, dbPort, sslMode)

	// Initialize GORM with PostgreSQL
	db, err := gormLib.Open(postgres.Open(dsn), &gormLib.Config{
		DisableForeignKeyConstraintWhenMigrating: true,
	})
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	fmt.Println("\n===== EXAMPLE QUERIES =====")

	// 1. Get all active Jobs for a company (latest version is active)
	fmt.Println("\n1. Get all active Jobs for a company:")
	var companyActiveJobs []Job
	companyID := "comp_cab5i8o0rvh5arskod" // Example company ID from your JSON data

	// Raw SQL query to get the latest version of each job for a company
	latestJobsQuery := `
	SELECT j.*
	FROM jobs j
	INNER JOIN (
		SELECT id, MAX(version) as max_version
		FROM jobs
		GROUP BY id
	) latest ON j.id = latest.id AND j.version = latest.max_version
	WHERE j.company_id = ? AND j.status = ?`

	if err := db.Raw(latestJobsQuery, companyID, "active").Scan(&companyActiveJobs).Error; err != nil {
		log.Printf("Failed to find active jobs for company: %v", err)
	} else {
		fmt.Printf("Found %d active jobs for company %s\n", len(companyActiveJobs), companyID)
		for i, j := range companyActiveJobs {
			fmt.Printf("  Job %d: %s (Contractor: %s, Rate: %.2f)\n", i+1, j.Title, j.ContractorID, j.Rate)
		}
	}

	// 2. Get all active Jobs for a contractor (latest version is active)
	fmt.Println("\n2. Get all active Jobs for a contractor:")
	var contractorActiveJobs []Job
	contractorID := "cont_e0nhseq682vkoc4d" // Example contractor ID from your JSON data

	// Raw SQL query to get the latest version of each job for a contractor
	if err := db.Raw(latestJobsQuery, contractorID, "active").Scan(&contractorActiveJobs).Error; err != nil {
		log.Printf("Failed to find active jobs for contractor: %v", err)
	} else {
		fmt.Printf("Found %d active jobs for contractor %s\n", len(contractorActiveJobs), contractorID)
		for i, j := range contractorActiveJobs {
			fmt.Printf("  Job %d: %s (Company: %s, Rate: %.2f)\n", i+1, j.Title, j.CompanyID, j.Rate)
		}
	}

	// 3. Get all PaymentLineItems for a contractor in a particular period of time (latest versions only)
	fmt.Println("\n3. Get all PaymentLineItems for a contractor in a time period:")

	// Parse example time period from your JSON data (March 2023)
	startTime, _ := time.Parse("2006-01-02", "2023-03-01")
	endTime, _ := time.Parse("2006-01-02", "2023-03-31")

	// First get the jobs for this contractor (latest versions only)
	var contractorJobs []Job
	contractorJobsQuery := `
	SELECT j.*
	FROM jobs j
	INNER JOIN (
		SELECT id, MAX(version) as max_version
		FROM jobs
		GROUP BY id
	) latest ON j.id = latest.id AND j.version = latest.max_version
	WHERE j.contractor_id = ?`

	if err := db.Raw(contractorJobsQuery, contractorID).Scan(&contractorJobs).Error; err != nil {
		log.Printf("Failed to find jobs for contractor: %v", err)
	} else {
		// Extract job UIDs to use in the payment line items query
		var jobUIDs []string
		for _, j := range contractorJobs {
			jobUIDs = append(jobUIDs, j.UID)
		}

		if len(jobUIDs) == 0 {
			fmt.Println("No jobs found for contractor, skipping payment line items query")
		} else {
			// Query payment line items linked to the contractor's jobs within the time period
			var contractorPayments []PaymentLineItem
			startTimeStr := startTime.Format("2006-01-02T15:04:05")
			endTimeStr := endTime.Format("2006-01-02T15:04:05")

			// Convert the array of UIDs to a comma-separated string for the SQL IN clause
			uidList := "'" + jobUIDs[0] + "'"
			for i := 1; i < len(jobUIDs); i++ {
				uidList += ", '" + jobUIDs[i] + "'"
			}

			paymentQuery := fmt.Sprintf(`
			SELECT p.*
			FROM payment_line_items p
			INNER JOIN (
				SELECT id, MAX(version) as max_version
				FROM payment_line_items
				GROUP BY id
			) latest ON p.id = latest.id AND p.version = latest.max_version
			WHERE p.job_uid IN (%s) 
			AND p.created_at BETWEEN '%s' AND '%s'`, uidList, startTimeStr, endTimeStr)

			if err := db.Raw(paymentQuery).Scan(&contractorPayments).Error; err != nil {
				log.Printf("Failed to find payment line items for contractor: %v", err)
			} else {
				fmt.Printf("Found %d payment line items for contractor %s between %s and %s\n",
					len(contractorPayments),
					contractorID,
					startTime.Format("2006-01-02"),
					endTime.Format("2006-01-02"))

				var totalAmount float64
				for i, p := range contractorPayments {
					fmt.Printf("  Payment %d: $%.2f (Status: %s)\n", i+1, p.Amount, p.Status)
					totalAmount += p.Amount
				}
				fmt.Printf("  Total Amount: $%.2f\n", totalAmount)
			}
		}
	}

	// 4. Get all Timelogs for a contractor in a particular period of time (latest versions only)
	fmt.Println("\n4. Get all Timelogs for a contractor in a time period:")

	// Using same job UIDs and time period as the previous query
	var contractorJobs2 []Job
	if err := db.Raw(contractorJobsQuery, contractorID).Scan(&contractorJobs2).Error; err != nil {
		log.Printf("Failed to find jobs for contractor: %v", err)
	} else {
		// Extract job UIDs to use in the timelogs query
		var jobUIDs []string
		for _, j := range contractorJobs2 {
			jobUIDs = append(jobUIDs, j.UID)
		}

		if len(jobUIDs) == 0 {
			fmt.Println("No jobs found for contractor, skipping timelogs query")
		} else {
			// Query timelogs linked to the contractor's jobs within the time period
			var contractorTimelogs []Timelog
			startTimeStr := startTime.Format("2006-01-02T15:04:05")
			endTimeStr := endTime.Format("2006-01-02T15:04:05")

			// Convert the array of UIDs to a comma-separated string for the SQL IN clause
			uidList := "'" + jobUIDs[0] + "'"
			for i := 1; i < len(jobUIDs); i++ {
				uidList += ", '" + jobUIDs[i] + "'"
			}

			timelogQuery := fmt.Sprintf(`
			SELECT t.*
			FROM timelogs t
			INNER JOIN (
				SELECT id, MAX(version) as max_version
				FROM timelogs
				GROUP BY id
			) latest ON t.id = latest.id AND t.version = latest.max_version
			WHERE t.job_uid IN (%s) 
			AND t.created_at BETWEEN '%s' AND '%s'`, uidList, startTimeStr, endTimeStr)

			if err := db.Raw(timelogQuery).Scan(&contractorTimelogs).Error; err != nil {
				log.Printf("Failed to find timelogs for contractor: %v", err)
			} else {
				fmt.Printf("Found %d timelogs for contractor %s between %s and %s\n",
					len(contractorTimelogs),
					contractorID,
					startTime.Format("2006-01-02"),
					endTime.Format("2006-01-02"))

				var totalHours float64
				for i, t := range contractorTimelogs {
					hours := float64(t.Duration) / 3600000.0 // Convert milliseconds to hours
					fmt.Printf("  Timelog %d: %.1f hours from %s to %s\n",
						i+1,
						hours,
						time.UnixMilli(t.TimeStart).Format("2006-01-02 15:04:05"),
						time.UnixMilli(t.TimeEnd).Format("2006-01-02 15:04:05"))
					totalHours += hours
				}
				fmt.Printf("  Total Hours: %.1f\n", totalHours)
			}
		}
	}
}

// Helper function to get environment variable with fallback
func getEnv(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}
