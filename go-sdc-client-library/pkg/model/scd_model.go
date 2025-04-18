package model

import (
	"time"
)

// SCDModel defines the basic fields for a Slowly Changing Dimension model
type SCDModel struct {
	ID        string    `gorm:"primaryKey" json:"id"`
	Version   int32     `gorm:"not null" json:"version"`
	UID       string    `gorm:"uniqueIndex" json:"uid"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// Versioned is an interface that all SCD models must implement
type Versioned interface {
	GetID() string
	SetID(id string)
	GetVersion() int32
	SetVersion(version int32)
	GetUID() string
	SetUID(uid string)
	GetCreatedAt() time.Time
	SetCreatedAt(createdAt time.Time)
	GetUpdatedAt() time.Time
	SetUpdatedAt(updatedAt time.Time)
	IsLatestVersion() bool
	SetLatestVersion(isLatest bool)
}

// SCDModelWithStatus extends SCDModel with a status field for soft deletion
type SCDModelWithStatus struct {
	SCDModel
	Status string `gorm:"default:active" json:"status"`
}

// Basic getter and setter implementations for SCDModel
func (m *SCDModel) GetID() string            { return m.ID }
func (m *SCDModel) SetID(id string)          { m.ID = id }
func (m *SCDModel) GetVersion() int32        { return m.Version }
func (m *SCDModel) SetVersion(version int32) { m.Version = version }
func (m *SCDModel) GetUID() string           { return m.UID }
func (m *SCDModel) SetUID(uid string)        { m.UID = uid }
func (m *SCDModel) GetCreatedAt() time.Time  { return m.CreatedAt }
func (m *SCDModel) SetCreatedAt(t time.Time) { m.CreatedAt = t }
func (m *SCDModel) GetUpdatedAt() time.Time  { return m.UpdatedAt }
func (m *SCDModel) SetUpdatedAt(t time.Time) { m.UpdatedAt = t }

// Additional fields for tracking versioning status
var (
	// This is not a database column, just a field name used internally in the code
	latestVersionField = "is_latest_version"
)

// Private field to track latest version status (not stored in database)
type scdModelExtension struct {
	IsLatest bool
}

// Embedded extension for each SCDModel
var modelExtensions = make(map[string]*scdModelExtension)

func (m *SCDModel) IsLatestVersion() bool {
	if ext, ok := modelExtensions[m.UID]; ok {
		return ext.IsLatest
	}
	return false
}

func (m *SCDModel) SetLatestVersion(isLatest bool) {
	if _, ok := modelExtensions[m.UID]; !ok {
		modelExtensions[m.UID] = &scdModelExtension{}
	}
	modelExtensions[m.UID].IsLatest = isLatest
}
