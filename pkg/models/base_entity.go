package models

import (
	"time"
)

// BaseSCDEntity provides a base implementation of SCDEntity
type BaseSCDEntity struct {
	ID        string    `gorm:"column:id;not null"`
	Version   int       `gorm:"column:version;not null"`
	UID       string    `gorm:"column:uid;primaryKey;not null"`
	CreatedAt time.Time `gorm:"column:created_at;not null"`
	UpdatedAt time.Time `gorm:"column:updated_at;not null"`
}

// GetID returns the entity ID
func (e *BaseSCDEntity) GetID() string {
	return e.ID
}

// SetID sets the entity ID
func (e *BaseSCDEntity) SetID(id string) {
	e.ID = id
}

// GetVersion returns the entity version
func (e *BaseSCDEntity) GetVersion() int {
	return e.Version
}

// SetVersion sets the entity version
func (e *BaseSCDEntity) SetVersion(version int) {
	e.Version = version
}

// GetUID returns the entity UID
func (e *BaseSCDEntity) GetUID() string {
	return e.UID
}

// SetUID sets the entity UID
func (e *BaseSCDEntity) SetUID(uid string) {
	e.UID = uid
}

// GetCreatedAt returns the entity creation time
func (e *BaseSCDEntity) GetCreatedAt() time.Time {
	return e.CreatedAt
}

// SetCreatedAt sets the entity creation time
func (e *BaseSCDEntity) SetCreatedAt(t time.Time) {
	e.CreatedAt = t
}

// GetUpdatedAt returns the entity update time
func (e *BaseSCDEntity) GetUpdatedAt() time.Time {
	return e.UpdatedAt
}

// SetUpdatedAt sets the entity update time
func (e *BaseSCDEntity) SetUpdatedAt(t time.Time) {
	e.UpdatedAt = t
}
