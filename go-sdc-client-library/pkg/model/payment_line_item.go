package model

import "time"

// PaymentLineItem represents a payment line item entity in the SCD service
type PaymentLineItem struct {
	SCDModel           // Embed SCD fields
	JobUID     string  `json:"job_uid"`
	TimelogUID string  `json:"timelog_uid"`
	Amount     float64 `json:"amount"`
	Status     string  `json:"status"`
}

// TableName returns the database table name for the PaymentLineItem model
func (PaymentLineItem) TableName() string {
	return "payment_line_items"
}

// EntityType returns the SCD entity type for the PaymentLineItem model
func (p *PaymentLineItem) EntityType() string {
	return "payment_line_item"
}

// IsPaid returns whether the payment has been paid
func (p *PaymentLineItem) IsPaid() bool {
	return p.Status == "paid"
}

// MarkAsPaid marks the payment as paid
func (p *PaymentLineItem) MarkAsPaid() {
	p.Status = "paid"
}

// FromMap creates a new PaymentLineItem from a map
func (p *PaymentLineItem) FromMap(data map[string]interface{}) {
	if id, ok := data["id"].(string); ok {
		p.ID = id
	}
	if version, ok := data["version"].(int32); ok {
		p.Version = version
	}
	if uid, ok := data["uid"].(string); ok {
		p.UID = uid
	}
	if createdAt, ok := data["createdAt"].(int64); ok {
		p.CreatedAt = time.Unix(0, createdAt*int64(time.Millisecond))
	}
	if updatedAt, ok := data["updatedAt"].(int64); ok {
		p.UpdatedAt = time.Unix(0, updatedAt*int64(time.Millisecond))
	}
	if jobUID, ok := data["jobUid"].(string); ok {
		p.JobUID = jobUID
	}
	if timelogUID, ok := data["timelogUid"].(string); ok {
		p.TimelogUID = timelogUID
	}
	if amount, ok := data["amount"].(float64); ok {
		p.Amount = amount
	}
	if status, ok := data["status"].(string); ok {
		p.Status = status
	}
}
