package job

import (
	"time"

	"github.com/mercor-ai/scd-go-client/pkg/models"
)

// Job represents a job entity
type Job struct {
	models.BaseSCDEntity
	Status       string  `gorm:"column:status;not null"`
	Rate         float64 `gorm:"column:rate;not null;type:decimal(10,2)"`
	Title        string  `gorm:"column:title;not null"`
	CompanyID    string  `gorm:"column:company_id;not null"`
	ContractorID string  `gorm:"column:contractor_id;not null"`
}

// TableName returns the table name for the entity
func (j *Job) TableName() string {
	return "jobs"
}

// CloneForNewVersion creates a new version of the job
func (j *Job) CloneForNewVersion(uid string, version int, now time.Time) models.SCDEntity {
	return &Job{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        j.ID,
			Version:   version,
			UID:       uid,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Status:       j.Status,
		Rate:         j.Rate,
		Title:        j.Title,
		CompanyID:    j.CompanyID,
		ContractorID: j.ContractorID,
	}
}
