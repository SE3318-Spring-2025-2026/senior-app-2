package com.seniorapp.controller;

import com.seniorapp.service.SecureOutboundApiService;
import com.seniorapp.util.SecureLogger;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Process 5.1 Controller - Secure outbound API calls with just-in-time token decryption.
 * Handles GitHub and Jira API requests with secure token management.
 */
@RestController
@RequestMapping("/api/process-51")
@CrossOrigin(origins = "*")
public class Process51Controller {
    
    private final SecureOutboundApiService outboundApiService;
    
    public Process51Controller(SecureOutboundApiService outboundApiService) {
        this.outboundApiService = outboundApiService;
    }
    
    /**
     * Process 5.1: Execute GitHub API call with secure token decryption
     */
    @PostMapping("/github")
    public ResponseEntity<String> executeGitHubApiCall(@RequestBody Process51Request request) {
        SecureLogger.logProcessStart("GitHub", "Controller Request", request.getApiEndpoint());
        
        try {
            // Validate encrypted token format before processing
            if (!outboundApiService.validateEncryptedTokenFormat(request.getEncryptedToken(), "GitHub")) {
                SecureLogger.logProcessFailure("GitHub", "Controller Request", "Invalid Token Format", 
                                               request.getApiEndpoint(), request.getMethod());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token format");
            }
            
            // Execute outbound API call with just-in-time decryption
            HttpMethod method = HttpMethod.valueOf(request.getMethod().toUpperCase());
            ResponseEntity<String> response = outboundApiService.executeGitHubApiCall(
                request.getEncryptedToken(), 
                request.getApiEndpoint(), 
                request.getRequestBody(), 
                method
            );
            
            SecureLogger.logProcessSuccess("GitHub", "Controller Request", response.getStatusCode().toString());
            return response;
            
        } catch (ResponseStatusException ex) {
            throw ex; // Re-throw our secure exceptions
        } catch (Exception ex) {
            SecureLogger.logProcessFailure("GitHub", "Controller Request", ex.getClass().getSimpleName(), 
                                         request.getApiEndpoint(), request.getMethod());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "GitHub API call failed");
        }
    }
    
    /**
     * Process 5.1: Execute Jira API call with secure token decryption
     */
    @PostMapping("/jira")
    public ResponseEntity<String> executeJiraApiCall(@RequestBody Process51Request request) {
        SecureLogger.logProcessStart("Jira", "Controller Request", request.getApiEndpoint());
        
        try {
            // Validate encrypted token format before processing
            if (!outboundApiService.validateEncryptedTokenFormat(request.getEncryptedToken(), "Jira")) {
                SecureLogger.logProcessFailure("Jira", "Controller Request", "Invalid Token Format", 
                                               request.getApiEndpoint(), request.getMethod());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token format");
            }
            
            // Execute outbound API call with just-in-time decryption
            HttpMethod method = HttpMethod.valueOf(request.getMethod().toUpperCase());
            ResponseEntity<String> response = outboundApiService.executeJiraApiCall(
                request.getEncryptedToken(), 
                request.getApiEndpoint(), 
                request.getRequestBody(), 
                method
            );
            
            SecureLogger.logProcessSuccess("Jira", "Controller Request", response.getStatusCode().toString());
            return response;
            
        } catch (ResponseStatusException ex) {
            throw ex; // Re-throw our secure exceptions
        } catch (Exception ex) {
            SecureLogger.logProcessFailure("Jira", "Controller Request", ex.getClass().getSimpleName(), 
                                         request.getApiEndpoint(), request.getMethod());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Jira API call failed");
        }
    }
    
    /**
     * Health check endpoint for Process 5.1 service
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"Process 5.1 service operational\"}");
    }
    
    /**
     * Request DTO for Process 5.1 API calls
     */
    public static class Process51Request {
        private String encryptedToken;
        private String apiEndpoint;
        private String method = "GET";
        private String requestBody;
        
        // Getters and setters
        public String getEncryptedToken() { return encryptedToken; }
        public void setEncryptedToken(String encryptedToken) { this.encryptedToken = encryptedToken; }
        
        public String getApiEndpoint() { return apiEndpoint; }
        public void setApiEndpoint(String apiEndpoint) { this.apiEndpoint = apiEndpoint; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getRequestBody() { return requestBody; }
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    }
}
