package models

import (
	"time"
)

// SCDEntity is the interface for all SCD entities
type SCDEntity interface {
	GetID() string
	SetID(string)
	
	GetVersion() int
	SetVersion(int)
	
	GetUID() string
	SetUID(string)
	
	GetCreatedAt() time.Time
	SetCreatedAt(time.Time)
	
	GetUpdatedAt() time.Time
	SetUpdatedAt(time.Time)
	
	TableName() string
	CloneForNewVersion(uid string, version int, now time.Time) SCDEntity
}
