package main

import (
	"flag"
	"fmt"
	"os"
	"os/exec"
	"strings"
)

func main() {
	// Define flags for each example type
	jobFlag := flag.Bool("job", false, "Run job examples")
	timelogFlag := flag.Bool("timelog", false, "Run timelog examples")
	paymentFlag := flag.Bool("payment", false, "Run payment line item examples")
	allFlag := flag.Bool("all", false, "Run all examples")
	testFlag := flag.Bool("test", false, "Run a simple test to verify gRPC client implementation")

	// Parse command-line flags
	flag.Parse()

	// If no flags specified, default to running the test
	if !*jobFlag && !*timelogFlag && !*paymentFlag && !*allFlag && !*testFlag {
		*testFlag = true
		fmt.Println("No examples specified. Running gRPC client test by default.")
	}

	fmt.Println("\n===== SCD CLIENT LIBRARY EXAMPLES =====")
	fmt.Println("Using real gRPC client implementation to communicate with SCD service")

	// Check if the proto files have been generated
	if !protoFilesExist() {
		fmt.Println("\n⚠️  gRPC generated files not found! Generating them now...")
		if err := generateProtoFiles(); err != nil {
			fmt.Printf("❌ Error generating proto files: %v\n", err)
			fmt.Println("Please run the generate-proto.sh script manually:")
			fmt.Println("  chmod +x scripts/generate-proto.sh")
			fmt.Println("  ./scripts/generate-proto.sh")
			os.Exit(1)
		}
		fmt.Println("✅ Proto files generated successfully!")
	}

	// Run the gRPC test if requested
	if *testFlag {
		fmt.Println("\nRunning gRPC Client Test:")
		// Call the test function
		fmt.Println("\nTest functionality is currently disabled.")
		// RunGRPCTest()
		return // Exit after the test
	}

	// Run selected examples
	if *allFlag || *jobFlag {
		fmt.Println("\n1. Job Examples:")
		// Call the job examples function
		fmt.Println("\nRunning job examples...")
		RunJobExamples()
	}

	if *allFlag || *timelogFlag {
		fmt.Println("\n2. Timelog Examples:")
		// Timelog examples are disabled in this demo.
		// Uncomment to enable timelog examples:
		fmt.Println("\nRunning timelog examples...")
		RunTimelogExamples()
	}

	if *allFlag || *paymentFlag {
		fmt.Println("\n3. Payment Examples:")
		// Payment examples are disabled in this demo.
		// Uncomment to enable payment examples:
		fmt.Println("\nRunning payment examples...")
		RunPaymentExamples()
	}

	fmt.Println("\nExamples completed successfully!")
}

// Helper function to get environment variable with fallback
func getEnvForMain(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}

// Check if the proto files have been generated
func protoFilesExist() bool {
	// Check if the proto files exist
	_, err1 := os.Stat("internal/proto/gen/scd_service.pb.go")
	_, err2 := os.Stat("internal/proto/gen/scd_service_grpc.pb.go")
	return err1 == nil && err2 == nil
}

// Generate the proto files
func generateProtoFiles() error {
	// Get the current working directory
	cwd, err := os.Getwd()
	if err != nil {
		return err
	}
	fmt.Println("Current directory:", cwd)

	// Change to the root directory if needed
	if !strings.HasSuffix(cwd, "go-client-library") {
		rootDir := findProjectRoot(cwd)
		if rootDir == "" {
			return fmt.Errorf("could not find project root directory")
		}

		fmt.Println("Changing to project root:", rootDir)
		if err := os.Chdir(rootDir); err != nil {
			return err
		}
	}

	// Run the generate-proto.sh script
	fmt.Println("Running generate-proto.sh...")
	cmd := exec.Command("/bin/sh", "-c", "chmod +x ./scripts/generate-proto.sh && ./scripts/generate-proto.sh")
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

// Find the project root directory
func findProjectRoot(dir string) string {
	// Check if we're in the examples/grpc directory
	if strings.HasSuffix(dir, "/examples/grpc") {
		return strings.TrimSuffix(dir, "/examples/grpc")
	}

	// Check if we're in the examples directory
	if strings.HasSuffix(dir, "/examples") {
		return strings.TrimSuffix(dir, "/examples")
	}

	// Check parent directories
	for {
		if _, err := os.Stat(dir + "/go.mod"); err == nil {
			return dir
		}

		newDir := getParentDir(dir)
		if newDir == dir {
			return ""
		}
		dir = newDir
	}
}

// Get the parent directory
func getParentDir(dir string) string {
	lastSlash := strings.LastIndex(dir, "/")
	if lastSlash <= 0 {
		return dir
	}
	return dir[:lastSlash]
}
