// pkg/models/payment/payment_line_item.go
package payment

import (
	"time"

	"github.com/mercor-ai/scd-go-client/pkg/models"
)

// PaymentLineItem represents a payment line item entity
type PaymentLineItem struct {
	models.BaseSCDEntity
	JobUID     string  `gorm:"column:job_uid;not null"`
	TimelogUID string  `gorm:"column:timelog_uid;not null"`
	Amount     float64 `gorm:"column:amount;not null;type:decimal(10,2)"`
	Status     string  `gorm:"column:status;not null"`
}

// TableName returns the table name for the entity
func (p *PaymentLineItem) TableName() string {
	return "payment_line_items"
}

// CloneForNewVersion creates a new version of the payment line item
func (p *PaymentLineItem) CloneForNewVersion(uid string, version int, now time.Time) models.SCDEntity {
	return &PaymentLineItem{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        p.ID,
			Version:   version,
			UID:       uid,
			CreatedAt: now,
			UpdatedAt: now,
		},
		JobUID:     p.JobUID,
		TimelogUID: p.TimelogUID,
		Amount:     p.Amount,
		Status:     p.Status,
	}
}
