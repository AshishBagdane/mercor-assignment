// pkg/repository/timelog_repository.go
package repository

import (
	"context"
	"fmt"
	"time"

	"gorm.io/gorm"

	"github.com/mercor-ai/scd-go-client/api/common"
	"github.com/mercor-ai/scd-go-client/api/core"
	"github.com/mercor-ai/scd-go-client/api/timelog"
	"github.com/mercor-ai/scd-go-client/pkg/client"
	"github.com/mercor-ai/scd-go-client/pkg/models"
	timelogmodel "github.com/mercor-ai/scd-go-client/pkg/models/timelog"
)

// TimelogRepository handles database operations for timelogs
type TimelogRepository struct {
	BaseRepository
	timelogClient timelog.TimelogServiceClient
	coreClient    core.SCDServiceClient
}

// NewTimelogRepository creates a new timelog repository
func NewTimelogRepository(db *gorm.DB, scdClient *client.Client) *TimelogRepository {
	return &TimelogRepository{
		BaseRepository: BaseRepository{
			DB:         db,
			SCDClient:  scdClient,
			EntityType: "timelog",
		},
		timelogClient: scdClient.Timelog(),
		coreClient:    scdClient.Core(),
	}
}

// GetTimelogRemote retrieves a timelog by ID from the SCD service
func (r *TimelogRepository) GetTimelogRemote(ctx context.Context, id string) (*timelogmodel.Timelog, error) {
	// Call SCD service to get latest version
	resp, err := r.coreClient.GetLatestVersion(ctx, &core.GetLatestVersionRequest{
		EntityType: "timelog",
		Id:         id,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get timelog from SCD service: %w", err)
	}

	// Convert response to model
	timelog := convertEntityToTimelog(resp.Entity)

	// Save to local database for caching
	if err := r.DB.Create(timelog).Error; err != nil {
		// Log error but continue - we still have the remote data
		fmt.Printf("Warning: Failed to cache timelog in local database: %v\n", err)
	}

	return timelog, nil
}

// AdjustTimelogRemote adjusts a timelog's duration through the SCD service
func (r *TimelogRepository) AdjustTimelogRemote(ctx context.Context, id string, duration int64) (*timelogmodel.Timelog, error) {
	// Call SCD service to adjust timelog
	resp, err := r.timelogClient.AdjustTimelog(ctx, &timelog.AdjustTimelogRequest{
		Id:               id,
		AdjustedDuration: duration,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to adjust timelog through SCD service: %w", err)
	}

	// Convert response to model
	adjustedTimelog := convertTimelogProtoToModel(resp.Timelog)

	// Save to local database for caching
	if err := r.DB.Create(adjustedTimelog).Error; err != nil {
		// Log error but continue - we still have the remote data
		fmt.Printf("Warning: Failed to cache adjusted timelog in local database: %v\n", err)
	}

	return adjustedTimelog, nil
}

// GetTimelogsForJobRemote retrieves timelogs for a job from the SCD service
func (r *TimelogRepository) GetTimelogsForJobRemote(ctx context.Context, jobUID string) ([]*timelogmodel.Timelog, error) {
	// Call SCD service to get timelogs for job
	resp, err := r.timelogClient.GetTimelogsForJob(ctx, &timelog.GetTimelogsForJobRequest{
		JobUid: jobUID,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get timelogs for job from SCD service: %w", err)
	}

	// Convert to model timelogs
	timelogs := make([]*timelogmodel.Timelog, 0, len(resp.Timelogs))
	for _, tlProto := range resp.Timelogs {
		timelogs = append(timelogs, convertTimelogProtoToModel(tlProto))
	}

	// Save to local database for caching
	if len(timelogs) > 0 {
		if err := r.DB.CreateInBatches(timelogs, 100).Error; err != nil {
			// Log error but continue - we still have the remote data
			fmt.Printf("Warning: Failed to cache timelogs in local database: %v\n", err)
		}
	}

	return timelogs, nil
}

// GetTimelogHistoryRemote retrieves timelog version history from the SCD service
func (r *TimelogRepository) GetTimelogHistoryRemote(ctx context.Context, id string) ([]*timelogmodel.Timelog, error) {
	// Call SCD service to get timelog history
	resp, err := r.coreClient.GetVersionHistory(ctx, &core.GetVersionHistoryRequest{
		EntityType: "timelogs",
		Id:         id,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get timelog history from SCD service: %w", err)
	}

	// Convert to model timelogs
	timelogs := make([]*timelogmodel.Timelog, 0, len(resp.Entities))
	for _, entity := range resp.Entities {
		timelogs = append(timelogs, convertEntityToTimelog(entity))
	}

	// Save to local database for caching
	if len(timelogs) > 0 {
		if err := r.DB.CreateInBatches(timelogs, 100).Error; err != nil {
			// Log error but continue - we still have the remote data
			fmt.Printf("Warning: Failed to cache timelog history in local database: %v\n", err)
		}
	}

	return timelogs, nil
}

// Helper functions

// convertTimelogProtoToModel converts a timelog proto to a timelog model
func convertTimelogProtoToModel(timelogProto *timelog.TimelogProto) *timelogmodel.Timelog {
	return &timelogmodel.Timelog{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        timelogProto.Id,
			Version:   int(timelogProto.Version),
			UID:       timelogProto.Uid,
			CreatedAt: time.Unix(0, timelogProto.CreatedAt*1000000),
			UpdatedAt: time.Unix(0, timelogProto.UpdatedAt*1000000),
		},
		Duration:  timelogProto.Duration,
		TimeStart: timelogProto.TimeStart,
		TimeEnd:   timelogProto.TimeEnd,
		Type:      timelogProto.Type,
		JobUID:    timelogProto.JobUid,
	}
}

// convertEntityToTimelog converts a generic entity to a timelog model
func convertEntityToTimelog(entity *common.Entity) *timelogmodel.Timelog {
	// In a real implementation, parse the entity.Data to get timelog-specific fields
	// For now, create a timelog with basic SCD fields set
	return &timelogmodel.Timelog{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        entity.Id,
			Version:   int(entity.Version),
			UID:       entity.Uid,
			CreatedAt: time.Unix(0, entity.CreatedAt*1000000),
			UpdatedAt: time.Unix(0, entity.UpdatedAt*1000000),
		},
		// Timelog-specific fields would come from parsing entity.Data
		Duration:  0,         // Placeholder
		TimeStart: 0,         // Placeholder
		TimeEnd:   0,         // Placeholder
		Type:      "unknown", // Placeholder
		JobUID:    "",        // Placeholder
	}
}
