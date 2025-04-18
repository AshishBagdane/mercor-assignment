package model

import "time"

// Job represents a job entity in the SCD service
type Job struct {
	SCDModel             // Embed SCD fields
	Status       string  `json:"status"`
	Rate         float64 `json:"rate"`
	Title        string  `json:"title"`
	CompanyID    string  `json:"company_id"`
	ContractorID string  `json:"contractor_id"`
}

// TableName returns the database table name for the Job model
func (Job) TableName() string {
	return "jobs"
}

// EntityType returns the SCD entity type for the Job model
func (Job) EntityType() string {
	return "job"
}

// Active returns whether the job is in an active state
func (j *Job) Active() bool {
	return j.Status == "active" || j.Status == "pending"
}

// Completed returns whether the job is completed
func (j *Job) Completed() bool {
	return j.Status == "completed"
}

// UpdateStatus updates the job's status
func (j *Job) UpdateStatus(status string) {
	j.Status = status
}

// UpdateRate updates the job's rate
func (j *Job) UpdateRate(rate float64) {
	j.Rate = rate
}

// FromMap creates a new Job from a map
func JobFromMap(data map[string]interface{}) (*Job, error) {
	job := &Job{}

	// Set SCD fields
	if id, ok := data["id"].(string); ok {
		job.ID = id
	}
	if version, ok := data["version"].(float64); ok {
		job.Version = int32(version)
	}
	if uid, ok := data["uid"].(string); ok {
		job.UID = uid
	}
	if createdAt, ok := data["createdAt"].(float64); ok {
		job.CreatedAt = time.Unix(int64(createdAt), 0)
	}
	if updatedAt, ok := data["updatedAt"].(float64); ok {
		job.UpdatedAt = time.Unix(int64(updatedAt), 0)
	}

	// Set Job fields
	if status, ok := data["status"].(string); ok {
		job.Status = status
	}
	if rate, ok := data["rate"].(float64); ok {
		job.Rate = rate
	}
	if title, ok := data["title"].(string); ok {
		job.Title = title
	}
	if companyID, ok := data["companyId"].(string); ok {
		job.CompanyID = companyID
	}
	if contractorID, ok := data["contractorId"].(string); ok {
		job.ContractorID = contractorID
	}

	return job, nil
}
