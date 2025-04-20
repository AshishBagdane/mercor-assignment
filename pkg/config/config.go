// pkg/config/config.go
package config

import (
	"fmt"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// DatabaseConfig holds database configuration
type DatabaseConfig struct {
	Host     string
	Port     int
	User     string
	Password string
	DBName   string
	SSLMode  string
}

// GRPCServerConfig holds gRPC server configuration
type GRPCServerConfig struct {
	Host         string
	Port         int
	Timeout      time.Duration
	DialOptions  []grpc.DialOption
	MaxRetries   int
	RetryBackoff time.Duration
}

// Config holds all configuration
type Config struct {
	Database DatabaseConfig
	GRPC     GRPCServerConfig
}

// DefaultConfig returns the default configuration
func DefaultConfig() *Config {
	return &Config{
		Database: DatabaseConfig{
			Host:     "localhost",
			Port:     5432,
			User:     "user",
			Password: "password",
			DBName:   "employment",
			SSLMode:  "disable",
		},
		GRPC: GRPCServerConfig{
			Host:         "localhost",
			Port:         50051,
			Timeout:      10 * time.Second,
			DialOptions:  []grpc.DialOption{grpc.WithTransportCredentials(insecure.NewCredentials())},
			MaxRetries:   3,
			RetryBackoff: 500 * time.Millisecond,
		},
	}
}

// GetDSN returns the database connection string
func (c *DatabaseConfig) GetDSN() string {
	return fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=%s",
		c.Host, c.Port, c.User, c.Password, c.DBName, c.SSLMode)
}

// GetServerAddress returns the gRPC server address
func (c *GRPCServerConfig) GetServerAddress() string {
	return fmt.Sprintf("%s:%d", c.Host, c.Port)
}
