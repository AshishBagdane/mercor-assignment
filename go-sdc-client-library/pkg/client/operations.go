package client

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/mercor-ai/go-sdc-client-library/pkg/model"

	// Import the generated gRPC client code
	pb "github.com/mercor-ai/go-sdc-client-library/internal/proto/gen"
)

// EntityType constants
const (
	EntityTypeJob             = "job"
	EntityTypeTimelog         = "timelog"
	EntityTypePaymentLineItem = "payment_line_item"
)

// GetLatestVersion retrieves the latest version of an entity
func (c *Client) GetLatestVersion(ctx context.Context, entityType, id string) (map[string]interface{}, error) {
	var result map[string]interface{}

	err := c.withRetry(ctx, func(ctx context.Context) error {
		// Create the request
		req := &pb.GetLatestVersionRequest{
			EntityType: entityType,
			Id:         id,
		}

		// Make the gRPC call
		resp, err := c.scdClient.GetLatestVersion(ctx, req)
		if err != nil {
			return err
		}

		// Convert the response to a map
		if resp == nil || resp.Entity == nil {
			return fmt.Errorf("%w: nil response", ErrInvalidResponse)
		}

		// Start with the basic entity fields
		result = map[string]interface{}{
			"id":        resp.Entity.Id,
			"type":      resp.Entity.Type,
			"version":   resp.Entity.Version,
			"uid":       resp.Entity.Uid,
			"createdAt": resp.Entity.CreatedAt,
			"updatedAt": resp.Entity.UpdatedAt,
		}

		// If there's additional data, unmarshal it
		if len(resp.Entity.Data) > 0 {
			var entityData map[string]interface{}
			if err := json.Unmarshal(resp.Entity.Data, &entityData); err != nil {
				return fmt.Errorf("failed to unmarshal entity data: %w", err)
			}

			// Merge entity data into result
			for k, v := range entityData {
				// Don't overwrite the basic fields
				if k != "id" && k != "type" && k != "version" &&
					k != "uid" && k != "createdAt" && k != "updatedAt" {
					result[k] = v
				}
			}
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	return result, nil
}

// GetVersionHistory retrieves all versions of an entity
func (c *Client) GetVersionHistory(ctx context.Context, entityType, id string) ([]map[string]interface{}, error) {
	var result []map[string]interface{}

	err := c.withRetry(ctx, func(ctx context.Context) error {
		// Create the request
		req := &pb.GetVersionHistoryRequest{
			EntityType: entityType,
			Id:         id,
		}

		// Make the gRPC call
		resp, err := c.scdClient.GetVersionHistory(ctx, req)
		if err != nil {
			return err
		}

		// Convert the response to a list of maps
		if resp == nil {
			return fmt.Errorf("%w: nil response", ErrInvalidResponse)
		}

		result = make([]map[string]interface{}, 0, len(resp.Entities))
		for _, entity := range resp.Entities {
			// Start with the basic entity fields
			entityMap := map[string]interface{}{
				"id":        entity.Id,
				"type":      entity.Type,
				"version":   entity.Version,
				"uid":       entity.Uid,
				"createdAt": entity.CreatedAt,
				"updatedAt": entity.UpdatedAt,
			}

			// If there's additional data, unmarshal it
			if len(entity.Data) > 0 {
				var entityData map[string]interface{}
				if err := json.Unmarshal(entity.Data, &entityData); err != nil {
					return fmt.Errorf("failed to unmarshal entity data: %w", err)
				}

				// Merge entity data into entityMap
				for k, v := range entityData {
					// Don't overwrite the basic fields
					if k != "id" && k != "type" && k != "version" &&
						k != "uid" && k != "createdAt" && k != "updatedAt" {
						entityMap[k] = v
					}
				}
			}

			result = append(result, entityMap)
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	return result, nil
}

// Query performs a query against the SCD service
func (c *Client) Query(ctx context.Context, entityType string, conditions map[string]interface{}, options QueryOptions) ([]map[string]interface{}, error) {
	var result []map[string]interface{}

	err := c.withRetry(ctx, func(ctx context.Context) error {
		// Convert conditions to string map
		conditionsMap := make(map[string]string)
		for k, v := range conditions {
			switch val := v.(type) {
			case string:
				conditionsMap[k] = val
			case float64:
				conditionsMap[k] = strconv.FormatFloat(val, 'f', -1, 64)
			case int:
				conditionsMap[k] = strconv.Itoa(val)
			case int64:
				conditionsMap[k] = strconv.FormatInt(val, 10)
			case bool:
				conditionsMap[k] = strconv.FormatBool(val)
			default:
				jsonVal, err := json.Marshal(val)
				if err != nil {
					return fmt.Errorf("failed to marshal condition value: %w", err)
				}
				conditionsMap[k] = string(jsonVal)
			}
		}

		// Create the request
		req := &pb.QueryRequest{
			EntityType:        entityType,
			Conditions:        conditionsMap,
			LatestVersionOnly: options.LatestVersionOnly,
			Limit:             int32(options.Limit),
			Offset:            int32(options.Offset),
			SortBy:            options.SortBy,
			SortDirection:     options.SortDirection,
		}

		// Make the gRPC call
		resp, err := c.scdClient.Query(ctx, req)
		if err != nil {
			return err
		}

		// Convert the response to a list of maps
		if resp == nil {
			return fmt.Errorf("%w: nil response", ErrInvalidResponse)
		}

		result = make([]map[string]interface{}, 0, len(resp.Entities))
		for _, entity := range resp.Entities {
			// Start with the basic entity fields
			entityMap := map[string]interface{}{
				"id":        entity.Id,
				"type":      entity.Type,
				"version":   entity.Version,
				"uid":       entity.Uid,
				"createdAt": entity.CreatedAt,
				"updatedAt": entity.UpdatedAt,
			}

			// If there's additional data, unmarshal it
			if len(entity.Data) > 0 {
				var entityData map[string]interface{}
				if err := json.Unmarshal(entity.Data, &entityData); err != nil {
					return fmt.Errorf("failed to unmarshal entity data: %w", err)
				}

				// Merge entity data into entityMap
				for k, v := range entityData {
					// Don't overwrite the basic fields
					if k != "id" && k != "type" && k != "version" &&
						k != "uid" && k != "createdAt" && k != "updatedAt" {
						entityMap[k] = v
					}
				}
			}

			result = append(result, entityMap)
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	return result, nil
}

// Update creates or updates an entity in the SCD service
func (c *Client) Update(ctx context.Context, entityType string, entity interface{}) (map[string]interface{}, error) {
	var result map[string]interface{}

	// Convert entity to a map for easier manipulation
	entityData, err := entityToMap(entity)
	if err != nil {
		return nil, fmt.Errorf("failed to convert entity to map: %w", err)
	}

	err = c.withRetry(ctx, func(ctx context.Context) error {
		// Extract ID and version from entity data
		id, _ := entityData["id"].(string)

		// Create the request
		req := &pb.UpdateRequest{
			EntityType: entityType,
			Id:         id,
			Fields:     make(map[string]string),
		}

		// Convert all fields to strings for the request
		for k, v := range entityData {
			if k != "id" && k != "type" && k != "uid" {
				switch val := v.(type) {
				case string:
					req.Fields[k] = val
				case float64:
					req.Fields[k] = strconv.FormatFloat(val, 'f', -1, 64)
				case int:
					req.Fields[k] = strconv.Itoa(val)
				case int32:
					req.Fields[k] = strconv.Itoa(int(val))
				case int64:
					req.Fields[k] = strconv.FormatInt(val, 10)
				case bool:
					req.Fields[k] = strconv.FormatBool(val)
				default:
					jsonVal, err := json.Marshal(val)
					if err != nil {
						return fmt.Errorf("failed to marshal field %s: %w", k, err)
					}
					req.Fields[k] = string(jsonVal)
				}
			}
		}

		// Serialize the entire entity as binary data
		entityJSON, err := json.Marshal(entityData)
		if err != nil {
			return fmt.Errorf("failed to marshal entity: %w", err)
		}

		// Create Entity object
		pbEntity := &pb.Entity{
			Type: entityType,
			Id:   id,
			Data: entityJSON,
		}

		// Set version if available
		if v, ok := entityData["version"]; ok {
			switch version := v.(type) {
			case int:
				pbEntity.Version = int32(version)
			case int32:
				pbEntity.Version = version
			case float64:
				pbEntity.Version = int32(version)
			}
		}

		req.Entity = pbEntity

		// Make the gRPC call
		resp, err := c.scdClient.Update(ctx, req)
		if err != nil {
			return err
		}

		// Convert the response to a map
		if resp == nil || resp.Entity == nil {
			return fmt.Errorf("%w: nil response", ErrInvalidResponse)
		}

		// Start with the basic entity fields
		result = map[string]interface{}{
			"id":        resp.Entity.Id,
			"type":      resp.Entity.Type,
			"version":   resp.Entity.Version,
			"uid":       resp.Entity.Uid,
			"createdAt": resp.Entity.CreatedAt,
			"updatedAt": resp.Entity.UpdatedAt,
		}

		// If there's additional data, unmarshal it
		if len(resp.Entity.Data) > 0 {
			var respData map[string]interface{}
			if err := json.Unmarshal(resp.Entity.Data, &respData); err != nil {
				return fmt.Errorf("failed to unmarshal response data: %w", err)
			}

			// Merge entity data into result
			for k, v := range respData {
				// Don't overwrite the basic fields
				if k != "id" && k != "type" && k != "version" &&
					k != "uid" && k != "createdAt" && k != "updatedAt" {
					result[k] = v
				}
			}
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	return result, nil
}

// BatchGet retrieves multiple entities at once
func (c *Client) BatchGet(ctx context.Context, entityType string, ids []string) (map[string]map[string]interface{}, error) {
	var result map[string]map[string]interface{}

	err := c.withRetry(ctx, func(ctx context.Context) error {
		// Create the request
		req := &pb.BatchGetRequest{
			EntityType: entityType,
			Ids:        ids,
		}

		// Make the gRPC call
		resp, err := c.scdClient.BatchGet(ctx, req)
		if err != nil {
			return err
		}

		// Convert the response to a map
		if resp == nil {
			return fmt.Errorf("%w: nil response", ErrInvalidResponse)
		}

		result = make(map[string]map[string]interface{})
		for _, entity := range resp.Entities {
			if entity == nil {
				continue
			}

			// Create a map for this entity
			entityMap := map[string]interface{}{
				"id":        entity.Id,
				"type":      entity.Type,
				"version":   entity.Version,
				"uid":       entity.Uid,
				"createdAt": entity.CreatedAt,
				"updatedAt": entity.UpdatedAt,
			}

			// If there's additional data, unmarshal it
			if len(entity.Data) > 0 {
				var entityData map[string]interface{}
				if err := json.Unmarshal(entity.Data, &entityData); err != nil {
					return fmt.Errorf("failed to unmarshal entity data: %w", err)
				}

				// Merge entity data into entityMap
				for k, v := range entityData {
					// Don't overwrite the basic fields
					if k != "id" && k != "type" && k != "version" &&
						k != "uid" && k != "createdAt" && k != "updatedAt" {
						entityMap[k] = v
					}
				}
			}

			// Add to result with ID as key
			result[entity.Id] = entityMap
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	return result, nil
}

// BatchUpdate updates multiple entities at once
func (c *Client) BatchUpdate(ctx context.Context, entityType string, entities []interface{}) (map[string]map[string]interface{}, error) {
	var result map[string]map[string]interface{}

	// Convert entities to maps for easier manipulation
	entityMaps := make([]map[string]interface{}, 0, len(entities))
	for _, entity := range entities {
		entityMap, err := entityToMap(entity)
		if err != nil {
			return nil, fmt.Errorf("failed to convert entity to map: %w", err)
		}
		entityMaps = append(entityMaps, entityMap)
	}

	err := c.withRetry(ctx, func(ctx context.Context) error {
		// Create the request
		req := &pb.BatchUpdateRequest{
			EntityType:   entityType,
			CommonFields: make(map[string]string),
			Entities:     make([]*pb.Entity, 0, len(entityMaps)),
		}

		// Convert each entity to a protobuf Entity
		for _, entityMap := range entityMaps {
			id, _ := entityMap["id"].(string)

			// Serialize the entity as binary data
			entityJSON, err := json.Marshal(entityMap)
			if err != nil {
				return fmt.Errorf("failed to marshal entity: %w", err)
			}

			// Create Entity object
			pbEntity := &pb.Entity{
				Type: entityType,
				Id:   id,
				Data: entityJSON,
			}

			// Set version if available
			if v, ok := entityMap["version"]; ok {
				switch version := v.(type) {
				case int:
					pbEntity.Version = int32(version)
				case int32:
					pbEntity.Version = version
				case float64:
					pbEntity.Version = int32(version)
				}
			}

			req.Entities = append(req.Entities, pbEntity)
		}

		// Make the gRPC call
		resp, err := c.scdClient.BatchUpdate(ctx, req)
		if err != nil {
			return err
		}

		// Convert the response to a map
		if resp == nil {
			return fmt.Errorf("%w: nil response", ErrInvalidResponse)
		}

		result = make(map[string]map[string]interface{})
		for _, entity := range resp.Entities {
			if entity == nil {
				continue
			}

			// Create a map for this entity
			entityMap := map[string]interface{}{
				"id":        entity.Id,
				"type":      entity.Type,
				"version":   entity.Version,
				"uid":       entity.Uid,
				"createdAt": entity.CreatedAt,
				"updatedAt": entity.UpdatedAt,
			}

			// If there's additional data, unmarshal it
			if len(entity.Data) > 0 {
				var entityData map[string]interface{}
				if err := json.Unmarshal(entity.Data, &entityData); err != nil {
					return fmt.Errorf("failed to unmarshal entity data: %w", err)
				}

				// Merge entity data into entityMap
				for k, v := range entityData {
					// Don't overwrite the basic fields
					if k != "id" && k != "type" && k != "version" &&
						k != "uid" && k != "createdAt" && k != "updatedAt" {
						entityMap[k] = v
					}
				}
			}

			// Add to result with ID as key
			result[entity.Id] = entityMap
		}

		return nil
	})

	if err != nil {
		return nil, err
	}

	return result, nil
}

// QueryOptions represents options for Query operations
type QueryOptions struct {
	LatestVersionOnly bool
	Limit             int
	Offset            int
	SortBy            string
	SortDirection     string
}

// Helper function to convert an entity to a map using JSON marshaling
func entityToMap(entity interface{}) (map[string]interface{}, error) {
	// Handle nil entity
	if entity == nil {
		return make(map[string]interface{}), nil
	}

	// Handle already-map entities
	if m, ok := entity.(map[string]interface{}); ok {
		return m, nil
	}

	// Marshal entity to JSON and then unmarshal to map
	data, err := json.Marshal(entity)
	if err != nil {
		return nil, err
	}

	var result map[string]interface{}
	if err := json.Unmarshal(data, &result); err != nil {
		return nil, err
	}

	return result, nil
}

// MapToEntity converts a map to an entity struct
func MapToEntity(data map[string]interface{}, entity interface{}) error {
	// Marshal map to JSON and then unmarshal to entity
	jsonData, err := json.Marshal(data)
	if err != nil {
		return err
	}

	if err := json.Unmarshal(jsonData, entity); err != nil {
		return err
	}

	// If entity implements Versioned interface, set latest version flag
	if v, ok := entity.(model.Versioned); ok {
		v.SetLatestVersion(true)
	}

	return nil
}
