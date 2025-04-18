package gorm

import (
	"context"
	"fmt"
	"reflect"
	"time"

	"github.com/mercor-ai/go-sdc-client-library/pkg/client"
	"gorm.io/gorm"
)

// registerCreateCallbacks registers callbacks for create operations
func registerCreateCallbacks(db *DB) error {
	// Before creating a new record, we need to prepare the SCD fields
	err := db.Callback().Create().Before("gorm:create").Register("scd:before_create", beforeCreate)
	if err != nil {
		return err
	}

	// After creating in the DB, we need to create in the SCD service
	err = db.Callback().Create().After("gorm:create").Register("scd:after_create", afterCreate(db.client))
	if err != nil {
		return err
	}

	return nil
}

// registerQueryCallbacks registers callbacks for query operations
func registerQueryCallbacks(db *DB) error {
	// Before querying, we need to add conditions for version filtering
	err := db.Callback().Query().Before("gorm:query").Register("scd:before_query", beforeQuery(db))
	if err != nil {
		return err
	}

	return nil
}

// registerUpdateCallbacks registers callbacks for update operations
func registerUpdateCallbacks(db *DB) error {
	// Before updating, we need to create a new version in the SCD service
	err := db.Callback().Update().Before("gorm:update").Register("scd:before_update", beforeUpdate(db.client))
	if err != nil {
		return err
	}

	// After updating in the DB, we need to update the SCD service
	err = db.Callback().Update().After("gorm:update").Register("scd:after_update", afterUpdate(db.client))
	if err != nil {
		return err
	}

	return nil
}

// registerDeleteCallbacks registers callbacks for delete operations
func registerDeleteCallbacks(db *DB) error {
	// Before deleting, mark the entity as deleted in the SCD service
	err := db.Callback().Delete().Before("gorm:delete").Register("scd:before_delete", beforeDelete(db.client))
	if err != nil {
		return err
	}

	return nil
}

// beforeCreate prepares a new record for creation
func beforeCreate(db *gorm.DB) {
	if db.Error != nil {
		return
	}

	// Extract the model value
	reflectValue := db.Statement.ReflectValue
	if reflectValue.Kind() == reflect.Ptr {
		reflectValue = reflectValue.Elem()
	}

	// Skip if this is not a struct or cannot be modified
	if reflectValue.Kind() != reflect.Struct || !reflectValue.CanAddr() {
		return
	}

	// Set initial values for SCD fields
	now := time.Now().Unix()
	for i := 0; i < reflectValue.NumField(); i++ {
		structField := reflectValue.Type().Field(i)

		// Check and set ID if empty
		if structField.Name == "ID" && reflectValue.Field(i).String() == "" {
			// Set ID based on GORM primary key or generate a new one
			if db.Statement.Schema != nil && len(db.Statement.Schema.PrimaryFieldDBNames) > 0 {
				for _, field := range db.Statement.Schema.PrimaryFields {
					if field.Name == "ID" {
						// Generate UUID or other ID based on your requirements
						reflectValue.Field(i).SetString(fmt.Sprintf("id-%d", now))
						break
					}
				}
			}
		}

		// Set version to 1 for new records
		if structField.Name == "Version" && reflectValue.Field(i).Int() == 0 {
			reflectValue.Field(i).SetInt(1)
		}

		// Set CreatedAt and UpdatedAt timestamps
		if structField.Name == "CreatedAt" && reflectValue.Field(i).Int() == 0 {
			reflectValue.Field(i).SetInt(now)
		}
		if structField.Name == "UpdatedAt" && reflectValue.Field(i).Int() == 0 {
			reflectValue.Field(i).SetInt(now)
		}
	}
}

// afterCreate handles SCD service creation after DB create
func afterCreate(scdClient *client.Client) func(db *gorm.DB) {
	return func(db *gorm.DB) {
		if db.Error != nil {
			return
		}

		// Extract the model and entity type
		entityType := getEntityType(db.Statement.Model)
		if entityType == "" {
			db.AddError(fmt.Errorf("could not determine entity type"))
			return
		}

		// Create the entity in the SCD service
		ctx := context.Background()
		_, err := scdClient.Update(ctx, entityType, db.Statement.ReflectValue.Interface())
		if err != nil {
			db.AddError(fmt.Errorf("failed to create entity in SCD service: %w", err))
			return
		}
	}
}

// beforeQuery adds conditions for version filtering
func beforeQuery(scdDB *DB) func(db *gorm.DB) {
	return func(db *gorm.DB) {
		if db.Error != nil {
			return
		}

		// Only add version filtering if this is not a history query
		if !scdDB.IsHistory() {
			// Use a subquery to get the latest version of each entity
			// instead of relying on a _latest_version column
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
				db.Where(subQuery)
			}
		}
	}
}

// beforeUpdate prepares an update operation
func beforeUpdate(scdClient *client.Client) func(db *gorm.DB) {
	return func(db *gorm.DB) {
		if db.Error != nil {
			return
		}

		// Extract the model and entity type
		entityType := getEntityType(db.Statement.Model)
		if entityType == "" {
			db.AddError(fmt.Errorf("could not determine entity type"))
			return
		}

		// This is a placeholder for the real SCD update logic
		// In a real implementation, we would create a new version in the SCD service

		// Update timestamps
		now := time.Now().Unix()
		db.Statement.SetColumn("UpdatedAt", now)

		// Increment version
		var currentVersion int32
		if db.Statement.Schema != nil {
			for _, field := range db.Statement.Schema.Fields {
				if field.Name == "Version" {
					// Get current version from the model
					currentVersion = int32(db.Statement.ReflectValue.FieldByName("Version").Int())
					// Increment version
					db.Statement.SetColumn("Version", currentVersion+1)
					break
				}
			}
		}
	}
}

// afterUpdate handles SCD service update after DB update
func afterUpdate(scdClient *client.Client) func(db *gorm.DB) {
	return func(db *gorm.DB) {
		if db.Error != nil {
			return
		}

		// Extract the model and entity type
		entityType := getEntityType(db.Statement.Model)
		if entityType == "" {
			db.AddError(fmt.Errorf("could not determine entity type"))
			return
		}

		// Update the entity in the SCD service
		ctx := context.Background()
		_, err := scdClient.Update(ctx, entityType, db.Statement.ReflectValue.Interface())
		if err != nil {
			db.AddError(fmt.Errorf("failed to update entity in SCD service: %w", err))
			return
		}
	}
}

// beforeDelete marks an entity as deleted
func beforeDelete(scdClient *client.Client) func(db *gorm.DB) {
	return func(db *gorm.DB) {
		if db.Error != nil {
			return
		}

		// Extract the model and entity type
		entityType := getEntityType(db.Statement.Model)
		if entityType == "" {
			db.AddError(fmt.Errorf("could not determine entity type"))
			return
		}

		// This is a placeholder for the real SCD delete logic
		// In a real implementation, we would mark the entity as deleted in the SCD service
		// by creating a new version with a "deleted" status

		// Soft delete in SCD - create a new version with status = deleted
		ctx := context.Background()
		_, err := scdClient.Update(ctx, entityType, map[string]interface{}{
			"id":     db.Statement.ReflectValue.FieldByName("ID").String(),
			"status": "deleted",
		})
		if err != nil {
			db.AddError(fmt.Errorf("failed to mark entity as deleted in SCD service: %w", err))
			return
		}
	}
}
