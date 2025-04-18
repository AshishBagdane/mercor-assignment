package com.mercor.assignment.sdc.common.config;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

/**
 * Web configuration for the application.
 * Provides documentation controller and gRPC reflection service.
 */
@Configuration
public class WebConfig {

    /**
     * Proto Reflection Service for dynamic gRPC discovery
     */
    @Bean
    public BindableService protoReflectionService() {
        return ProtoReflectionService.newInstance();
    }

    /**
     * Controller for API documentation and health checks
     */
    @RestController
    public static class ApiDocController {

        @Value("${grpc.server.port:9090}")
        private int grpcServerPort;

        @Value("${application.api.version:1.0.0}")
        private String apiVersion;

        /**
         * Redirect root URL to the API documentation
         */
        @GetMapping("/")
        public ModelAndView redirectToApiDocs() {
            return new ModelAndView("redirect:/api-docs");
        }

        /**
         * Health check endpoint
         */
        @GetMapping("/health")
        public String health() {
            return "OK";
        }

        /**
         * gRPC API documentation page
         */
        @GetMapping(value = "/api-docs", produces = MediaType.TEXT_HTML_VALUE)
        public String grpcApiDocs() {
            return "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "  <title>SDC Service gRPC API Documentation</title>\n" +
                    "  <style>\n" +
                    "    body { font-family: Arial, sans-serif; line-height: 1.6; margin: 0; padding: 20px; color: #333; }\n"
                    +
                    "    h1 { color: #0275d8; }\n" +
                    "    h2 { color: #5cb85c; margin-top: 30px; }\n" +
                    "    h3 { color: #5bc0de; }\n" +
                    "    code { background: #f8f9fa; padding: 2px 5px; border-radius: 3px; }\n" +
                    "    pre { background: #f8f9fa; padding: 15px; border-radius: 5px; overflow-x: auto; }\n" +
                    "    .service { margin-bottom: 40px; border-bottom: 1px solid #eee; padding-bottom: 20px; }\n" +
                    "    .method { margin-bottom: 20px; }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <h1>SDC Service gRPC API Documentation</h1>\n" +
                    "  <p>This page provides documentation for the gRPC services available in this application.</p>\n" +
                    "  <p>API Version: <code>" + apiVersion + "</code></p>\n" +
                    "  <p>gRPC server is running on port: <code>" + grpcServerPort + "</code></p>\n" +
                    "  \n" +
                    "  <h2>Available gRPC Services</h2>\n" +
                    "  \n" +
                    "  <div class=\"service\">\n" +
                    "    <h3>SCDService</h3>\n" +
                    "    <p>Base service for SCD operations</p>\n" +
                    "    \n" +
                    "    <div class=\"method\">\n" +
                    "      <h4>GetLatestVersion</h4>\n" +
                    "      <p>Get the latest version of an entity by ID</p>\n" +
                    "      <pre>rpc GetLatestVersion (GetLatestVersionRequest) returns (EntityResponse);</pre>\n" +
                    "    </div>\n" +
                    "    \n" +
                    "    <div class=\"method\">\n" +
                    "      <h4>GetVersionHistory</h4>\n" +
                    "      <p>Get the complete history of an entity by ID</p>\n" +
                    "      <pre>rpc GetVersionHistory (GetVersionHistoryRequest) returns (EntityListResponse);</pre>\n"
                    +
                    "    </div>\n" +
                    "    \n" +
                    "    <!-- More methods can be added here -->\n" +
                    "  </div>\n" +
                    "  \n" +
                    "  <div class=\"service\">\n" +
                    "    <h3>JobService</h3>\n" +
                    "    <p>Operations related to Jobs</p>\n" +
                    "    \n" +
                    "    <div class=\"method\">\n" +
                    "      <h4>GetActiveJobsForCompany</h4>\n" +
                    "      <p>Get active jobs for a company</p>\n" +
                    "      <pre>rpc GetActiveJobsForCompany (GetActiveJobsForCompanyRequest) returns (JobListResponse);</pre>\n"
                    +
                    "    </div>\n" +
                    "    \n" +
                    "    <!-- More methods can be added here -->\n" +
                    "  </div>\n" +
                    "  \n" +
                    "  <div class=\"service\">\n" +
                    "    <h3>TimelogService</h3>\n" +
                    "    <p>Operations related to Timelogs</p>\n" +
                    "    \n" +
                    "    <div class=\"method\">\n" +
                    "      <h4>GetTimelogsForJob</h4>\n" +
                    "      <p>Get timelogs for a job</p>\n" +
                    "      <pre>rpc GetTimelogsForJob (GetTimelogsForJobRequest) returns (TimelogListResponse);</pre>\n"
                    +
                    "    </div>\n" +
                    "    \n" +
                    "    <!-- More methods can be added here -->\n" +
                    "  </div>\n" +
                    "  \n" +
                    "  <div class=\"service\">\n" +
                    "    <h3>PaymentLineItemService</h3>\n" +
                    "    <p>Operations related to PaymentLineItems</p>\n" +
                    "    \n" +
                    "    <div class=\"method\">\n" +
                    "      <h4>GetPaymentLineItemsForJob</h4>\n" +
                    "      <p>Get payment line items for a job</p>\n" +
                    "      <pre>rpc GetPaymentLineItemsForJob (GetPaymentLineItemsForJobRequest) returns (PaymentLineItemListResponse);</pre>\n"
                    +
                    "    </div>\n" +
                    "    \n" +
                    "    <!-- More methods can be added here -->\n" +
                    "  </div>\n" +
                    "  \n" +
                    "  <h2>Using gRPC Clients</h2>\n" +
                    "  <p>To interact with these services, use a gRPC client like:</p>\n" +
                    "  <ul>\n" +
                    "    <li><a href=\"https://github.com/fullstorydev/grpcurl\">grpcurl</a></li>\n" +
                    "    <li><a href=\"https://github.com/bloomrpc/bloomrpc\">BloomRPC</a></li>\n" +
                    "    <li><a href=\"https://github.com/fullstorydev/grpcui\">gRPCui</a></li>\n" +
                    "  </ul>\n" +
                    "  \n" +
                    "  <h3>Example grpcurl commands</h3>\n" +
                    "  <pre>grpcurl -plaintext localhost:" + grpcServerPort + " list\n" +
                    "grpcurl -plaintext localhost:" + grpcServerPort + " describe com.mercor.scd.grpc.JobService\n" +
                    "grpcurl -plaintext -d '{\"company_id\": \"123\"}' localhost:" + grpcServerPort
                    + " com.mercor.scd.grpc.JobService/GetActiveJobsForCompany</pre>\n" +
                    "</body>\n" +
                    "</html>";
        }
    }
}