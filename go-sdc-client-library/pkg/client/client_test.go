package client

import (
	"context"
	"testing"
	"time"
)

func TestNewClient(t *testing.T) {
	// Test with default config
	c, err := NewClient()
	if err != nil {
		t.Fatalf("Failed to create client with default config: %v", err)
	}
	if c == nil {
		t.Fatal("Expected non-nil client")
	}
	if c.config.Host != DefaultSCDHost {
		t.Errorf("Expected host %s, got %s", DefaultSCDHost, c.config.Host)
	}
	if c.config.Port != DefaultSCDPort {
		t.Errorf("Expected port %d, got %d", DefaultSCDPort, c.config.Port)
	}
	c.Close()

	// Test with target
	c, err = NewClientWithTarget("example.com")
	if err != nil {
		t.Fatalf("Failed to create client with target: %v", err)
	}
	if c == nil {
		t.Fatal("Expected non-nil client")
	}
	if c.config.Host != "example.com" {
		t.Errorf("Expected host example.com, got %s", c.config.Host)
	}
	if c.config.Port != DefaultSCDPort {
		t.Errorf("Expected default port %d, got %d", DefaultSCDPort, c.config.Port)
	}
	c.Close()

	// Test with host and port
	c, err = NewClientWithHostPort("test.example.com", 8080)
	if err != nil {
		t.Fatalf("Failed to create client with host and port: %v", err)
	}
	if c == nil {
		t.Fatal("Expected non-nil client")
	}
	if c.config.Host != "test.example.com" {
		t.Errorf("Expected host test.example.com, got %s", c.config.Host)
	}
	if c.config.Port != 8080 {
		t.Errorf("Expected port 8080, got %d", c.config.Port)
	}
	c.Close()

	// Test with custom config
	config := ClientConfig{
		Timeout:    10 * time.Second,
		MaxRetries: 5,
		RetryDelay: 200 * time.Millisecond,
		Host:       "custom.example.com",
		Port:       9000,
	}
	c, err = NewClientWithConfig(config)
	if err != nil {
		t.Fatalf("Failed to create client with custom config: %v", err)
	}
	if c == nil {
		t.Fatal("Expected non-nil client")
	}
	if c.config.Timeout != 10*time.Second {
		t.Errorf("Expected timeout of 10s, got %v", c.config.Timeout)
	}
	if c.config.MaxRetries != 5 {
		t.Errorf("Expected max retries of 5, got %d", c.config.MaxRetries)
	}
	if c.config.RetryDelay != 200*time.Millisecond {
		t.Errorf("Expected retry delay of 200ms, got %v", c.config.RetryDelay)
	}
	if c.config.Host != "custom.example.com" {
		t.Errorf("Expected host custom.example.com, got %s", c.config.Host)
	}
	if c.config.Port != 9000 {
		t.Errorf("Expected port 9000, got %d", c.config.Port)
	}
	c.Close()
}

func TestGetTarget(t *testing.T) {
	// Test with default values
	c, _ := NewClient()
	expectedTarget := "localhost:9090"
	if target := c.GetTarget(); target != expectedTarget {
		t.Errorf("Expected target %s, got %s", expectedTarget, target)
	}
	c.Close()

	// Test with custom values
	c, _ = NewClientWithHostPort("scd.example.com", 8888)
	expectedTarget = "scd.example.com:8888"
	if target := c.GetTarget(); target != expectedTarget {
		t.Errorf("Expected target %s, got %s", expectedTarget, target)
	}
	c.Close()
}

func TestEntityToMap(t *testing.T) {
	// Test with nil
	m, err := entityToMap(nil)
	if err != nil {
		t.Fatalf("entityToMap failed with nil: %v", err)
	}
	if len(m) != 0 {
		t.Errorf("Expected empty map for nil entity, got %v", m)
	}

	// Test with map
	inputMap := map[string]interface{}{
		"id":      "123",
		"version": 1,
	}
	m, err = entityToMap(inputMap)
	if err != nil {
		t.Fatalf("entityToMap failed with map: %v", err)
	}
	if m["id"] != "123" || m["version"] != 1 {
		t.Errorf("Map values not preserved, got %v", m)
	}

	// Test with struct
	type TestEntity struct {
		ID      string `json:"id"`
		Version int    `json:"version"`
		Name    string `json:"name"`
	}
	entity := TestEntity{
		ID:      "456",
		Version: 2,
		Name:    "Test",
	}
	m, err = entityToMap(entity)
	if err != nil {
		t.Fatalf("entityToMap failed with struct: %v", err)
	}
	if m["id"] != "456" || m["version"] != float64(2) || m["name"] != "Test" {
		t.Errorf("Struct values not correctly converted, got %v", m)
	}
}

func TestMapToEntity(t *testing.T) {
	// Create a test map
	data := map[string]interface{}{
		"id":        "123",
		"version":   float64(2),
		"uid":       "job-123-v2",
		"createdAt": float64(1634567890),
		"updatedAt": float64(1634657890),
		"status":    "active",
		"rate":      float64(50.0),
	}

	// Define a simple entity
	type TestEntity struct {
		ID        string  `json:"id"`
		Version   int32   `json:"version"`
		UID       string  `json:"uid"`
		CreatedAt int64   `json:"createdAt"`
		UpdatedAt int64   `json:"updatedAt"`
		Status    string  `json:"status"`
		Rate      float64 `json:"rate"`
	}

	// Convert map to entity
	var entity TestEntity
	err := MapToEntity(data, &entity)
	if err != nil {
		t.Fatalf("MapToEntity failed: %v", err)
	}

	// Verify fields
	if entity.ID != "123" {
		t.Errorf("Expected ID 123, got %s", entity.ID)
	}
	if entity.Version != 2 {
		t.Errorf("Expected Version 2, got %d", entity.Version)
	}
	if entity.UID != "job-123-v2" {
		t.Errorf("Expected UID job-123-v2, got %s", entity.UID)
	}
	if entity.Status != "active" {
		t.Errorf("Expected Status active, got %s", entity.Status)
	}
	if entity.Rate != 50.0 {
		t.Errorf("Expected Rate 50.0, got %f", entity.Rate)
	}
}

func TestWithRetry(t *testing.T) {
	// Create a client with a short timeout for testing
	c, err := NewClientWithConfig(ClientConfig{
		Timeout:    100 * time.Millisecond,
		MaxRetries: 2,
		RetryDelay: 50 * time.Millisecond,
		Host:       DefaultSCDHost,
		Port:       DefaultSCDPort,
	})
	if err != nil {
		t.Fatalf("Failed to create client: %v", err)
	}
	defer c.Close()

	// Test successful operation
	attempt := 0
	err = c.withRetry(context.Background(), func(ctx context.Context) error {
		attempt++
		return nil // Success
	})
	if err != nil {
		t.Errorf("Expected no error, got %v", err)
	}
	if attempt != 1 {
		t.Errorf("Expected 1 attempt, got %d", attempt)
	}

	// Test operation that fails once then succeeds
	attempt = 0
	err = c.withRetry(context.Background(), func(ctx context.Context) error {
		attempt++
		if attempt == 1 {
			return ErrServiceUnavailable
		}
		return nil
	})
	if err != nil {
		t.Errorf("Expected no error after retry, got %v", err)
	}
	if attempt != 2 {
		t.Errorf("Expected 2 attempts, got %d", attempt)
	}

	// Test operation that always fails
	attempt = 0
	err = c.withRetry(context.Background(), func(ctx context.Context) error {
		attempt++
		return ErrServiceUnavailable
	})
	if err != ErrServiceUnavailable {
		t.Errorf("Expected ErrServiceUnavailable, got %v", err)
	}
	if attempt != 3 { // Initial + 2 retries
		t.Errorf("Expected 3 attempts, got %d", attempt)
	}
}
