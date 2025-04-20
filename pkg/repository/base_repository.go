package repository

import (
	"context"
	"fmt"
	"time"

	"gorm.io/gorm"

	"github.com/mercor-ai/scd-go-client/pkg/client"
	"github.com/mercor-ai/scd-go-client/pkg/models"
	"github.com/mercor-ai/scd-go-client/pkg/scd"
)

// BaseRepository provides common SCD operations for all entity repositories
type BaseRepository struct {
	DB         *gorm.DB
	SCDClient  *client.Client
	EntityType string
}

// GetLatest retrieves the latest version of an entity by ID
func (r *BaseRepository) GetLatest(ctx context.Context, id string, dest models.SCDEntity) error {
	// Get latest version
	err := r.DB.WithContext(ctx).
		Where("id = ?", id).
		Order("version DESC").
		First(dest).Error

	if err != nil {
		return fmt.Errorf("failed to get latest version: %w", err)
	}

	return nil
}

// FindLatest retrieves entities matching the conditions (latest versions only)
func (r *BaseRepository) FindLatest(ctx context.Context, dest interface{}, conditions ...interface{}) error {
	// Build subquery to get the latest version IDs
	subQuery := r.DB.Table(r.TableName()).
		Select("id, MAX(version) as version").
		Group("id")

	// Apply conditions if provided
	query := r.DB.WithContext(ctx).
		Table("(?) as latest", subQuery).
		Joins(fmt.Sprintf("JOIN %s ON %s.id = latest.id AND %s.version = latest.version",
			r.TableName(), r.TableName(), r.TableName()))

	if len(conditions) > 0 {
		if len(conditions) == 1 {
			query = query.Where(conditions[0])
		} else {
			query = query.Where(conditions[0], conditions[1:]...)
		}
	}

	return query.Find(dest).Error
}

// Create creates a new entity (first version)
func (r *BaseRepository) Create(ctx context.Context, entity models.SCDEntity) error {
	// Begin transaction
	tx := r.DB.WithContext(ctx).Begin()
	if tx.Error != nil {
		return tx.Error
	}
	defer func() {
		if r := recover(); r != nil {
			tx.Rollback()
		}
	}()

	// Set initial SCD fields
	now := time.Now()
	entity.SetVersion(1)
	entity.SetCreatedAt(now)
	entity.SetUpdatedAt(now)

	// Generate entity ID if not set
	if entity.GetID() == "" {
		generator := scd.NewUIDGenerator()
		id, err := generator.GenerateEntityID(r.EntityType)
		if err != nil {
			tx.Rollback()
			return err
		}
		entity.SetID(id)
	}

	// Generate UID
	generator := scd.NewUIDGenerator()
	uid, err := generator.GenerateUID(r.EntityType)
	if err != nil {
		tx.Rollback()
		return err
	}
	entity.SetUID(uid)

	// Save to database
	if err := tx.Create(entity).Error; err != nil {
		tx.Rollback()
		return err
	}

	// Commit transaction
	return tx.Commit().Error
}

// Update creates a new version of an entity
func (r *BaseRepository) Update(ctx context.Context, entity models.SCDEntity) error {
	// Begin transaction
	tx := r.DB.WithContext(ctx).Begin()
	if tx.Error != nil {
		return tx.Error
	}
	defer func() {
		if r := recover(); r != nil {
			tx.Rollback()
		}
	}()

	// Get current latest version
	var currentVersion int
	if err := tx.Model(entity).
		Where("id = ?", entity.GetID()).
		Select("MAX(version)").
		Scan(&currentVersion).Error; err != nil {
		tx.Rollback()
		return err
	}

	// Generate new UID
	generator := scd.NewUIDGenerator()
	uid, err := generator.GenerateUID(r.EntityType)
	if err != nil {
		tx.Rollback()
		return err
	}

	// Clone entity with new version
	now := time.Now()
	newVersion := entity.CloneForNewVersion(uid, currentVersion+1, now)

	// Save to database
	if err := tx.Create(newVersion).Error; err != nil {
		tx.Rollback()
		return err
	}

	// Update the original entity with new values
	entity.SetUID(newVersion.GetUID())
	entity.SetVersion(newVersion.GetVersion())
	entity.SetCreatedAt(newVersion.GetCreatedAt())
	entity.SetUpdatedAt(newVersion.GetUpdatedAt())

	// Commit transaction
	return tx.Commit().Error
}

// GetHistory retrieves all versions of an entity
func (r *BaseRepository) GetHistory(ctx context.Context, id string, dest interface{}) error {
	return r.DB.WithContext(ctx).
		Where("id = ?", id).
		Order("version ASC").
		Find(dest).Error
}

// TableName returns the table name for the entity type
func (r *BaseRepository) TableName() string {
	return r.EntityType
}
