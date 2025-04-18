package model

import "time"

// Timelog represents a timelog entity in the SCD service
type Timelog struct {
	SCDModel         // Embed SCD fields
	Duration  int64  `json:"duration"`
	TimeStart int64  `json:"time_start"`
	TimeEnd   int64  `json:"time_end"`
	Type      string `json:"type"`
	JobUID    string `json:"job_uid"`
}

// TableName returns the database table name for the Timelog model
func (Timelog) TableName() string {
	return "timelogs"
}

// EntityType returns the SCD entity type for the Timelog model
func (t *Timelog) EntityType() string {
	return "timelog"
}

// DurationHours returns the duration in hours
func (t *Timelog) DurationHours() float64 {
	return float64(t.Duration) / (1000 * 60 * 60)
}

// AdjustDuration updates the timelog's duration and end time
func (t *Timelog) AdjustDuration(newDuration int64) {
	t.Duration = newDuration
	t.TimeEnd = t.TimeStart + newDuration
}

// FromMap creates a new Timelog from a map
func (t *Timelog) FromMap(data map[string]interface{}) {
	if id, ok := data["id"].(string); ok {
		t.ID = id
	}
	if version, ok := data["version"].(int32); ok {
		t.Version = version
	}
	if uid, ok := data["uid"].(string); ok {
		t.UID = uid
	}
	if createdAt, ok := data["createdAt"].(int64); ok {
		t.CreatedAt = time.Unix(0, createdAt*int64(time.Millisecond))
	}
	if updatedAt, ok := data["updatedAt"].(int64); ok {
		t.UpdatedAt = time.Unix(0, updatedAt*int64(time.Millisecond))
	}
	if duration, ok := data["duration"].(int64); ok {
		t.Duration = duration
	}
	if timeStart, ok := data["timeStart"].(int64); ok {
		t.TimeStart = timeStart
	}
	if timeEnd, ok := data["timeEnd"].(int64); ok {
		t.TimeEnd = timeEnd
	}
	if type_, ok := data["type"].(string); ok {
		t.Type = type_
	}
	if jobUID, ok := data["jobUid"].(string); ok {
		t.JobUID = jobUID
	}
}
