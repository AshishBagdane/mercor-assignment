package client

import (
	"time"
	
	"google.golang.org/grpc"
)

// Config holds client configuration options
type Config struct {
	// Server address (host:port)
	ServerAddress string
	
	// Additional gRPC dial options
	DialOptions []grpc.DialOption
	
	// Timeout for requests (0 means no timeout)
	Timeout time.Duration
}
