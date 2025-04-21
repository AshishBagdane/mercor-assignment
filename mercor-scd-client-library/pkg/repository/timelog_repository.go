// pkg/repository/timelog_repository.go
package repository

import (
	"context"
	"encoding/json"
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

	return timelogs, nil
}

// GetTimelogHistoryRemote retrieves timelog version history from the SCD service
func (r *TimelogRepository) GetTimelogHistoryRemote(ctx context.Context, id string) ([]*timelogmodel.Timelog, error) {
	// Call SCD service to get timelog history
	resp, err := r.coreClient.GetVersionHistory(ctx, &core.GetVersionHistoryRequest{
		EntityType: "timelog",
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
	// Create timelog with basic SCD fields
	timelog := &timelogmodel.Timelog{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        entity.Id,
			Version:   int(entity.Version),
			UID:       entity.Uid,
			CreatedAt: time.Unix(0, entity.CreatedAt*1000000),
			UpdatedAt: time.Unix(0, entity.UpdatedAt*1000000),
		},
	}

	// Decode the data to get timelog-specific fields
	if len(entity.Data) > 0 {
		// Parse JSON data
		var timelogData map[string]interface{}
		if err := json.Unmarshal(entity.Data, &timelogData); err == nil {
			// Extract timelog fields
			if duration, ok := timelogData["duration"].(float64); ok {
				timelog.Duration = int64(duration)
			}
			if timeStart, ok := timelogData["timeStart"].(float64); ok {
				timelog.TimeStart = int64(timeStart)
			}
			if timeEnd, ok := timelogData["timeEnd"].(float64); ok {
				timelog.TimeEnd = int64(timeEnd)
			}
			if type_, ok := timelogData["type"].(string); ok {
				timelog.Type = type_
			}
			if jobUID, ok := timelogData["jobUid"].(string); ok {
				timelog.JobUID = jobUID
			}
		}
	}

	return timelog
}
