package client

import (
	"context"
	"errors"
	"fmt"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"

	// Import the generated gRPC client code
	pb "github.com/mercor-ai/go-sdc-client-library/internal/proto/gen"
)

// Default configuration values
const (
	DefaultTimeout    = 30 * time.Second
	DefaultMaxRetries = 3
	DefaultRetryDelay = 500 * time.Millisecond
	DefaultSCDHost    = "localhost"
	DefaultSCDPort    = 9090
)

// Errors
var (
	ErrConnectionFailed   = errors.New("failed to connect to SCD service")
	ErrServiceUnavailable = errors.New("SCD service is unavailable")
	ErrInvalidResponse    = errors.New("received invalid response from SCD service")
	ErrTimeout            = errors.New("request to SCD service timed out")
)

// ClientConfig contains configuration options for the SCD client
type ClientConfig struct {
	Timeout    time.Duration
	MaxRetries int
	RetryDelay time.Duration
	Host       string
	Port       int
}

// Client represents a client for the SCD service
type Client struct {
	conn          *grpc.ClientConn
	config        ClientConfig
	scdClient     pb.SCDServiceClient
	jobClient     pb.JobServiceClient
	timelogClient pb.TimelogServiceClient
	paymentClient pb.PaymentLineItemServiceClient
}

// NewClient creates a new SCD client with default configuration
func NewClient() (*Client, error) {
	return NewClientWithConfig(ClientConfig{
		Timeout:    DefaultTimeout,
		MaxRetries: DefaultMaxRetries,
		RetryDelay: DefaultRetryDelay,
		Host:       DefaultSCDHost,
		Port:       DefaultSCDPort,
	})
}

// NewClientWithTarget creates a new SCD client with a specific target but default settings
func NewClientWithTarget(target string) (*Client, error) {
	return NewClientWithConfig(ClientConfig{
		Timeout:    DefaultTimeout,
		MaxRetries: DefaultMaxRetries,
		RetryDelay: DefaultRetryDelay,
		Host:       target,
		Port:       DefaultSCDPort,
	})
}

// NewClientWithHostPort creates a new SCD client with specific host and port
func NewClientWithHostPort(host string, port int) (*Client, error) {
	return NewClientWithConfig(ClientConfig{
		Timeout:    DefaultTimeout,
		MaxRetries: DefaultMaxRetries,
		RetryDelay: DefaultRetryDelay,
		Host:       host,
		Port:       port,
	})
}

// NewClientWithConfig creates a new SCD client with custom configuration
func NewClientWithConfig(config ClientConfig) (*Client, error) {
	// Set default values for unspecified config options
	if config.Timeout == 0 {
		config.Timeout = DefaultTimeout
	}
	if config.MaxRetries == 0 {
		config.MaxRetries = DefaultMaxRetries
	}
	if config.RetryDelay == 0 {
		config.RetryDelay = DefaultRetryDelay
	}
	if config.Host == "" {
		config.Host = DefaultSCDHost
	}
	if config.Port == 0 {
		config.Port = DefaultSCDPort
	}

	// Create the target address from host and port
	target := fmt.Sprintf("%s:%d", config.Host, config.Port)

	// Create a connection to the gRPC server
	conn, err := grpc.Dial(target, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		return nil, fmt.Errorf("%w: %v", ErrConnectionFailed, err)
	}

	// Create the service clients
	client := &Client{
		conn:          conn,
		config:        config,
		scdClient:     pb.NewSCDServiceClient(conn),
		jobClient:     pb.NewJobServiceClient(conn),
		timelogClient: pb.NewTimelogServiceClient(conn),
		paymentClient: pb.NewPaymentLineItemServiceClient(conn),
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

// GetTarget returns the target address of the SCD service
func (c *Client) GetTarget() string {
	return fmt.Sprintf("%s:%d", c.config.Host, c.config.Port)
}

// withRetry executes the given function with retry logic
func (c *Client) withRetry(ctx context.Context, fn func(context.Context) error) error {
	var lastErr error
	for attempt := 0; attempt <= c.config.MaxRetries; attempt++ {
		// Create a timeout context for this attempt
		timeoutCtx, cancel := context.WithTimeout(ctx, c.config.Timeout)
		defer cancel()

		// Execute the function
		err := fn(timeoutCtx)
		if err == nil {
			return nil
		}

		lastErr = err
		// Don't sleep on the last attempt
		if attempt < c.config.MaxRetries {
			time.Sleep(c.config.RetryDelay)
		}
	}

	return lastErr
}
