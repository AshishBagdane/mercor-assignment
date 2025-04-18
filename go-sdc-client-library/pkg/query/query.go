package query

import (
	"fmt"

	"gorm.io/gorm"
)

// LatestOnly creates a scope that filters for the latest version of entities
func LatestOnly(db *gorm.DB) *gorm.DB {
	// Get the table name from the statement
	tableName := db.Statement.Table
	if tableName == "" && db.Statement.Model != nil {
		// Try to get table name from the model
		stmt := &gorm.Statement{DB: db}
		_ = stmt.Parse(db.Statement.Model)
		tableName = stmt.Table
	}

	if tableName != "" {
		subQuery := fmt.Sprintf(
			"id IN (SELECT t1.id FROM %s t1 INNER JOIN (SELECT id, MAX(version) as max_version FROM %s GROUP BY id) t2 ON t1.id = t2.id AND t1.version = t2.max_version)",
			tableName, tableName)
		return db.Where(subQuery)
	}

	// If we couldn't determine the table name, just return the original DB
	return db
}

// WithVersion creates a scope that filters for a specific version of entities
func WithVersion(version int32) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("version = ?", version)
	}
}

// WithVersionRange creates a scope that filters for entities within a version range
func WithVersionRange(minVersion, maxVersion int32) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where("version >= ? AND version <= ?", minVersion, maxVersion)
	}
}

// WithDateRange creates a scope that filters for entities created or updated within a date range
func WithDateRange(field string, startTimestamp, endTimestamp int64) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		validFields := map[string]bool{
			"created_at": true,
			"updated_at": true,
		}

		if !validFields[field] {
			// If field is invalid, don't apply any filter and return warning
			fmt.Printf("Warning: Invalid field %s for date range filter. Use 'created_at' or 'updated_at'.\n", field)
			return db
		}

		return db.Where(fmt.Sprintf("%s >= ? AND %s <= ?", field, field), startTimestamp, endTimestamp)
	}
}

// VersionedSoftDelete creates a scope that filters out soft-deleted entities
func VersionedSoftDelete(db *gorm.DB) *gorm.DB {
	return db.Where("status <> ?", "deleted")
}

// BatchScope provides a way to efficiently process large datasets in batches
func BatchScope(batchSize int, callback func(tx *gorm.DB) error) func(db *gorm.DB) error {
	return func(db *gorm.DB) error {
		var offset int
		for {
			// Create a new transaction for each batch
			tx := db.Session(&gorm.Session{})

			// Apply limit and offset
			batchDB := tx.Limit(batchSize).Offset(offset)

			// Execute callback with batch scope
			if err := callback(batchDB); err != nil {
				return err
			}

			// Move to next batch
			offset += batchSize

			// Check if we've processed all records
			var count int64
			if err := batchDB.Count(&count).Error; err != nil {
				return err
			}

			if count < int64(batchSize) {
				break
			}
		}
		return nil
	}
}

// RelatedEntities creates a scope that finds entities related to another entity
func RelatedEntities(entityType, foreignKey, referenceID string) func(db *gorm.DB) *gorm.DB {
	return func(db *gorm.DB) *gorm.DB {
		return db.Where(fmt.Sprintf("%s = ?", foreignKey), referenceID)
	}
}
