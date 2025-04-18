package main

import (
	"fmt"
	"time"

	"gorm.io/gorm"
)

// generateUniqueID creates a unique ID string suitable for database entities
func generateUniqueID() string {
	now := time.Now()
	return fmt.Sprintf("%x%x", now.UnixNano(), time.Now().UnixNano()%1000)[0:16]
}

// DisableLatestVersionFilter creates a DB session that skips the callbacks that add version filtering
// This is useful when you want to query all versions or use custom version filtering
func DisableLatestVersionFilter(db *gorm.DB) *gorm.DB {
	// Create a new session that skips hooks
	return db.Session(&gorm.Session{
		SkipHooks: true, // Skip all callbacks including the one that adds version filtering
	})
}
