package gorm

import (
	"errors"
	"fmt"
	"reflect"

	"github.com/mercor-ai/go-sdc-client-library/pkg/client"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// Errors
var (
	ErrInvalidDialector = errors.New("invalid dialector")
	ErrInvalidConfig    = errors.New("invalid configuration")
	ErrClientRequired   = errors.New("SCD client is required")
)

// Config contains configuration options for the SCD GORM integration
type Config struct {
	*gorm.Config // Embed standard GORM config
	SCDClient    *client.Client
}

// Model is a shorthand for embedding in model structs
type Model struct {
	ID        string `gorm:"primaryKey"`
	Version   int32  `gorm:"not null"`
	UID       string `gorm:"uniqueIndex"`
	CreatedAt int64
	UpdatedAt int64
}

// DB wraps gorm.DB with SCD extensions
type DB struct {
	*gorm.DB // Embed standard GORM DB
	client   *client.Client
	version  bool // Flag to include version history in queries
}

// Open initializes a new SCD-aware GORM DB connection
func Open(dialector gorm.Dialector, config *Config) (*DB, error) {
	if dialector == nil {
		return nil, ErrInvalidDialector
	}
	if config == nil {
		return nil, ErrInvalidConfig
	}
	if config.SCDClient == nil {
		return nil, ErrClientRequired
	}

	// Configure standard GORM settings if not provided
	if config.Config == nil {
		config.Config = &gorm.Config{
			Logger: logger.Default.LogMode(logger.Info),
		}
	}

	// Open GORM connection
	db, err := gorm.Open(dialector, config.Config)
	if err != nil {
		return nil, err
	}

	// Create SCD-aware DB wrapper
	scdDB := &DB{
		DB:      db,
		client:  config.SCDClient,
		version: false,
	}

	// Register callbacks
	if err := registerCallbacks(scdDB); err != nil {
		return nil, err
	}

	return scdDB, nil
}

// History returns a new DB that includes version history in queries
func (db *DB) History() *DB {
	newDB := &DB{
		DB:      db.Session(&gorm.Session{}),
		client:  db.client,
		version: true,
	}
	return newDB
}

// Model overrides gorm.DB's Model method to ensure version tracking
func (db *DB) Model(value interface{}) *DB {
	newDB := &DB{
		DB:      db.DB.Model(value),
		client:  db.client,
		version: db.version,
	}
	return newDB
}

// Client returns the underlying SCD client
func (db *DB) Client() *client.Client {
	return db.client
}

// IsHistory returns whether this DB is configured to include version history
func (db *DB) IsHistory() bool {
	return db.version
}

// getEntityType determines the entity type from the model
func getEntityType(model interface{}) string {
	if model == nil {
		return ""
	}

	modelType := reflect.TypeOf(model)
	if modelType.Kind() == reflect.Ptr {
		modelType = modelType.Elem()
	}

	// Default to lowercase struct name
	typeName := modelType.Name()
	return convertCamelToSnake(typeName)
}

// convertCamelToSnake converts camel case to snake case
func convertCamelToSnake(s string) string {
	result := ""
	for i, r := range s {
		if i > 0 && r >= 'A' && r <= 'Z' {
			result += "_"
		}
		result += string(r)
	}
	return result
}

// registerCallbacks registers all necessary GORM callbacks for SCD functionality
func registerCallbacks(db *DB) error {
	if err := registerCreateCallbacks(db); err != nil {
		return fmt.Errorf("failed to register create callbacks: %w", err)
	}
	if err := registerQueryCallbacks(db); err != nil {
		return fmt.Errorf("failed to register query callbacks: %w", err)
	}
	if err := registerUpdateCallbacks(db); err != nil {
		return fmt.Errorf("failed to register update callbacks: %w", err)
	}
	if err := registerDeleteCallbacks(db); err != nil {
		return fmt.Errorf("failed to register delete callbacks: %w", err)
	}
	return nil
}
