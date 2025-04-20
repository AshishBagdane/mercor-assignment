// pkg/models/timelog/timelog.go
package timelog

import (
	"time"

	"github.com/mercor-ai/scd-go-client/pkg/models"
)

// Timelog represents a timelog entity
type Timelog struct {
	models.BaseSCDEntity
	Duration  int64  `gorm:"column:duration;not null"`
	TimeStart int64  `gorm:"column:time_start;not null"`
	TimeEnd   int64  `gorm:"column:time_end;not null"`
	Type      string `gorm:"column:type;not null"`
	JobUID    string `gorm:"column:job_uid;not null"`
}

// TableName returns the table name for the entity
func (t *Timelog) TableName() string {
	return "timelogs"
}

// CloneForNewVersion creates a new version of the timelog
func (t *Timelog) CloneForNewVersion(uid string, version int, now time.Time) models.SCDEntity {
	return &Timelog{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        t.ID,
			Version:   version,
			UID:       uid,
			CreatedAt: now,
			UpdatedAt: now,
		},
		Duration:  t.Duration,
		TimeStart: t.TimeStart,
		TimeEnd:   t.TimeEnd,
		Type:      t.Type,
		JobUID:    t.JobUID,
	}
}
