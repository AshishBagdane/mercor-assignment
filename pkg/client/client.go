// pkg/client/client.go
package client

import (
	"context"
	"fmt"
	"time"

	"google.golang.org/grpc"

	"github.com/mercor-ai/scd-go-client/api/core"
	"github.com/mercor-ai/scd-go-client/api/job"
	"github.com/mercor-ai/scd-go-client/api/paymentlineitems"
	"github.com/mercor-ai/scd-go-client/api/timelog"
)

// Client is the main client for SCD services
type Client struct {
	conn           *grpc.ClientConn
	config         Config
	coreService    core.SCDServiceClient
	jobService     job.JobServiceClient
	timelogService timelog.TimelogServiceClient
	paymentService paymentlineitems.PaymentLineItemServiceClient
}

// New creates a new SCD client
func New(config Config) (*Client, error) {
	// Set default timeout if not specified
	if config.Timeout == 0 {
		config.Timeout = 10 * time.Second
	}

	// Establish gRPC connection
	ctx, cancel := context.WithTimeout(context.Background(), config.Timeout)
	defer cancel()

	conn, err := grpc.DialContext(ctx, config.ServerAddress, config.DialOptions...)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to server: %w", err)
	}

	// Create service clients
	coreService := core.NewSCDServiceClient(conn)
	jobService := job.NewJobServiceClient(conn)
	timelogService := timelog.NewTimelogServiceClient(conn)
	paymentService := paymentlineitems.NewPaymentLineItemServiceClient(conn)

	// Create client
	client := &Client{
		conn:           conn,
		config:         config,
		coreService:    coreService,
		jobService:     jobService,
		timelogService: timelogService,
		paymentService: paymentService,
	}

	return client, nil
}

// Close closes the client connection
func (c *Client) Close() error {
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}

// Core returns the core service client
func (c *Client) Core() core.SCDServiceClient {
	return c.coreService
}

// Job returns the job service client
func (c *Client) Job() job.JobServiceClient {
	return c.jobService
}

// Timelog returns the timelog service client
func (c *Client) Timelog() timelog.TimelogServiceClient {
	return c.timelogService
}

// Payment returns the payment line item service client
func (c *Client) Payment() paymentlineitems.PaymentLineItemServiceClient {
	return c.paymentService
}
