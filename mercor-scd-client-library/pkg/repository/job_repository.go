// pkg/repository/job_repository.go
package repository

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"gorm.io/gorm"

	"github.com/mercor-ai/scd-go-client/api/common"
	"github.com/mercor-ai/scd-go-client/api/core"
	"github.com/mercor-ai/scd-go-client/api/job"
	"github.com/mercor-ai/scd-go-client/pkg/client"
	"github.com/mercor-ai/scd-go-client/pkg/models"
	jobmodel "github.com/mercor-ai/scd-go-client/pkg/models/job"
)

// JobRepository handles database operations for jobs
type JobRepository struct {
	BaseRepository
	jobClient  job.JobServiceClient
	coreClient core.SCDServiceClient
}

// NewJobRepository creates a new job repository
func NewJobRepository(db *gorm.DB, scdClient *client.Client) *JobRepository {
	return &JobRepository{
		BaseRepository: BaseRepository{
			DB:         db,
			SCDClient:  scdClient,
			EntityType: "jobs",
		},
		jobClient:  scdClient.Job(),
		coreClient: scdClient.Core(),
	}
}

// GetActiveJobsForCompanyRemote retrieves active jobs for a company from the SCD service
func (r *JobRepository) GetActiveJobsForCompanyRemote(ctx context.Context, companyID string) ([]*jobmodel.Job, error) {
	// Call the SCD service
	resp, err := r.jobClient.GetActiveJobsForCompany(ctx, &job.GetActiveJobsForCompanyRequest{
		CompanyId: companyID,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get active jobs from SCD service: %w", err)
	}

	// Convert to model jobs
	jobs := make([]*jobmodel.Job, 0, len(resp.Jobs))
	for _, jobProto := range resp.Jobs {
		jobs = append(jobs, convertJobProtoToModel(jobProto))
	}

	return jobs, nil
}

// GetActiveJobsForContractorRemote retrieves active jobs for a contractor from the SCD service
func (r *JobRepository) GetActiveJobsForContractorRemote(ctx context.Context, contractorID string) ([]*jobmodel.Job, error) {
	// Call the SCD service
	resp, err := r.jobClient.GetActiveJobsForContractor(ctx, &job.GetActiveJobsForContractorRequest{
		ContractorId: contractorID,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get active jobs from SCD service: %w", err)
	}

	// Convert to model jobs
	jobs := make([]*jobmodel.Job, 0, len(resp.Jobs))
	for _, jobProto := range resp.Jobs {
		jobs = append(jobs, convertJobProtoToModel(jobProto))
	}

	// Save to local database for caching
	if len(jobs) > 0 {
		if err := r.DB.CreateInBatches(jobs, 100).Error; err != nil {
			// Log error but continue - we still have the remote data
			fmt.Printf("Warning: Failed to cache jobs in local database: %v\n", err)
		}
	}

	return jobs, nil
}

// CreateJobRemote creates a new job through the SCD service
func (r *JobRepository) CreateJobRemote(ctx context.Context, j *jobmodel.Job) (*jobmodel.Job, error) {
	// Create UpdateRequest directly without creating an unused jobProto variable
	updateReq := &core.UpdateRequest{
		EntityType: "jobs",
		Id:         "", // New entity, no ID yet
		Fields: map[string]string{
			"status":        j.Status,
			"title":         j.Title,
			"company_id":    j.CompanyID,
			"contractor_id": j.ContractorID,
			"rate":          fmt.Sprintf("%f", j.Rate), // Convert rate to string
		},
	}

	resp, err := r.coreClient.Update(ctx, updateReq)
	if err != nil {
		return nil, fmt.Errorf("failed to create job through SCD service: %w", err)
	}

	// Convert response to model
	createdJob := convertEntityToJob(resp.Entity)

	return createdJob, nil
}

// UpdateJobStatusRemote updates a job's status through the SCD service
func (r *JobRepository) UpdateJobStatusRemote(ctx context.Context, id string, status string) (*jobmodel.Job, error) {
	// Call SCD service to update job
	resp, err := r.jobClient.UpdateStatus(ctx, &job.UpdateJobStatusRequest{
		Id:     id,
		Status: status,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to update job status through SCD service: %w", err)
	}

	// Convert response to model
	updatedJob := convertJobProtoToModel(resp.Job)

	return updatedJob, nil
}

// GetJobHistoryRemote retrieves job version history from the SCD service
func (r *JobRepository) GetJobHistoryRemote(ctx context.Context, id string) ([]*jobmodel.Job, error) {
	// Call SCD service to get job history
	resp, err := r.coreClient.GetVersionHistory(ctx, &core.GetVersionHistoryRequest{
		EntityType: "jobs",
		Id:         id,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get job history from SCD service: %w", err)
	}

	// Convert to model jobs
	jobs := make([]*jobmodel.Job, 0, len(resp.Entities))
	for _, entity := range resp.Entities {
		jobs = append(jobs, convertEntityToJob(entity))
	}

	return jobs, nil
}

// Helper functions

// convertJobProtoToModel converts a job proto to a job model
func convertJobProtoToModel(jobProto *job.JobProto) *jobmodel.Job {
	return &jobmodel.Job{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        jobProto.Id,
			Version:   int(jobProto.Version),
			UID:       jobProto.Uid,
			CreatedAt: time.Unix(0, jobProto.CreatedAt*1000000),
			UpdatedAt: time.Unix(0, jobProto.UpdatedAt*1000000),
		},
		Status:       jobProto.Status,
		Rate:         jobProto.Rate,
		Title:        jobProto.Title,
		CompanyID:    jobProto.CompanyId,
		ContractorID: jobProto.ContractorId,
	}
}

// convertEntityToJob converts a generic entity to a job model
func convertEntityToJob(entity *common.Entity) *jobmodel.Job {
	// Create job with basic SCD fields
	job := &jobmodel.Job{
		BaseSCDEntity: models.BaseSCDEntity{
			ID:        entity.Id,
			Version:   int(entity.Version),
			UID:       entity.Uid,
			CreatedAt: time.Unix(0, entity.CreatedAt*1000000),
			UpdatedAt: time.Unix(0, entity.UpdatedAt*1000000),
		},
	}

	// Decode the data to get job-specific fields
	if len(entity.Data) > 0 {
		var jobData map[string]interface{}
		if err := json.Unmarshal(entity.Data, &jobData); err == nil {
			// Extract job fields
			if status, ok := jobData["status"].(string); ok {
				job.Status = status
			}
			if title, ok := jobData["title"].(string); ok {
				job.Title = title
			}
			if companyId, ok := jobData["companyId"].(string); ok {
				job.CompanyID = companyId
			}
			if contractorId, ok := jobData["contractorId"].(string); ok {
				job.ContractorID = contractorId
			}
			if rate, ok := jobData["rate"].(float64); ok {
				job.Rate = rate
			}
		}
	}

	return job
}

// GetJobRemote retrieves a job by ID from the SCD service
func (r *JobRepository) GetJobRemote(ctx context.Context, id string) (*jobmodel.Job, error) {
	// Call SCD service to get latest version
	resp, err := r.coreClient.GetLatestVersion(ctx, &core.GetLatestVersionRequest{
		EntityType: "jobs",
		Id:         id,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get job from SCD service: %w", err)
	}

	// Convert response to model
	job := convertEntityToJob(resp.Entity)

	return job, nil
}
