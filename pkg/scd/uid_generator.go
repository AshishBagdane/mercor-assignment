package scd

import (
	"crypto/rand"
	"encoding/base64"
	"fmt"
)

// UIDGenerator generates unique IDs for SCD entities
type UIDGenerator struct{}

// NewUIDGenerator creates a new UID generator
func NewUIDGenerator() *UIDGenerator {
	return &UIDGenerator{}
}

// GenerateUID generates a unique ID for an entity version
func (g *UIDGenerator) GenerateUID(entityType string) (string, error) {
	// Generate random bytes
	randomBytes := make([]byte, 16)
	if _, err := rand.Read(randomBytes); err != nil {
		return "", err
	}
	
	// Encode to base64 URL-safe string without padding
	encodedID := base64.RawURLEncoding.EncodeToString(randomBytes)
	
	// Format: {entityType}_uid_{random-string}
	return fmt.Sprintf("%s_uid_%s", entityType, encodedID), nil
}

// GenerateEntityID generates a new entity ID
func (g *UIDGenerator) GenerateEntityID(entityType string) (string, error) {
	// Generate random bytes
	randomBytes := make([]byte, 16)
	if _, err := rand.Read(randomBytes); err != nil {
		return "", err
	}
	
	// Encode to base64 URL-safe string without padding
	encodedID := base64.RawURLEncoding.EncodeToString(randomBytes)
	
	// Format: {entityType}_{random-string}
	return fmt.Sprintf("%s_%s", entityType, encodedID), nil
}
