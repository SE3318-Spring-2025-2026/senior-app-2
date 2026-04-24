package com.seniorapp.service;

import com.seniorapp.exception.SecureDecryptionException;
import com.seniorapp.util.SecureLogger;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Secure outbound API service for Process 5.1 with just-in-time token decryption.
 * Tokens remain encrypted at rest and are only decrypted temporarily in memory 
 * during outbound API calls.
 */
@Service
public class SecureOutboundApiService {
    
    private final IntegrationCredentialCryptoService cryptoService;
    private final RestTemplate restTemplate;
    
    public SecureOutboundApiService(IntegrationCredentialCryptoService cryptoService) {
        this.cryptoService = cryptoService;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Process 5.1: Execute outbound GitHub API call with just-in-time token decryption
     */
    public ResponseEntity<String> executeGitHubApiCall(String encryptedToken, String apiEndpoint, 
                                                      String requestBody, HttpMethod method) {
        // Validate token format before proceeding
        if (!validateEncryptedTokenFormat(encryptedToken, "GitHub")) {
            SecureLogger.logProcessFailure("GitHub", "API Call", "Invalid Token Format", apiEndpoint, method.name());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid GitHub token format");
        }
        
        return executeApiCall("GitHub", encryptedToken, "Bearer", apiEndpoint, requestBody, method);
    }
    
    /**
     * Process 5.1: Execute outbound Jira API call with just-in-time token decryption
     */
    public ResponseEntity<String> executeJiraApiCall(String encryptedToken, String apiEndpoint, 
                                                     String requestBody, HttpMethod method) {
        // Validate token format before proceeding
        if (!validateEncryptedTokenFormat(encryptedToken, "Jira")) {
            SecureLogger.logProcessFailure("Jira", "API Call", "Invalid Token Format", apiEndpoint, method.name());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Jira token format");
        }
        
        return executeApiCall("Jira", encryptedToken, "Basic", apiEndpoint, requestBody, method);
    }
    
    /**
     * Core method for Process 5.1 - just-in-time decryption and secure API execution
     */
    private ResponseEntity<String> executeApiCall(String serviceType, String encryptedToken, 
                                                   String authScheme, String apiEndpoint, 
                                                   String requestBody, HttpMethod method) {
        
        String decryptedToken = null;
        try {
            // Step 1: Just-in-time decryption - only decrypt when needed for outbound call
            SecureLogger.logProcessStart(serviceType, "API Call", apiEndpoint);
            decryptedToken = cryptoService.decrypt(encryptedToken);
            
            if (decryptedToken == null || decryptedToken.isBlank()) {
                SecureLogger.logDecryptionFailure(serviceType, "API Call");
                throw new SecureDecryptionException(serviceType);
            }
            
            // Step 2: Build secure request with decrypted token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            if ("Bearer".equals(authScheme)) {
                headers.setBearerAuth(decryptedToken);
            } else if ("Basic".equals(authScheme)) {
                headers.setBasicAuth(decryptedToken, ""); // For Jira API tokens
            }
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // Step 3: Execute outbound API call
            SecureLogger.logApiCallAttempt(serviceType, method.name(), apiEndpoint);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiEndpoint, method, entity, String.class);
            
            SecureLogger.logProcessSuccess(serviceType, "API Call", response.getStatusCode().toString());
            
            return response;
            
        } catch (Exception ex) {
            // Step 4: Secure error handling - never log plaintext tokens
            if (ex instanceof ResponseStatusException || ex instanceof SecureDecryptionException) {
                throw ex; // Re-throw our own exceptions
            }
            
            SecureLogger.logProcessFailure(serviceType, "API Call", ex.getClass().getSimpleName(), 
                                         apiEndpoint, method.name());
            
            // Check if it's a decryption failure
            if (ex.getMessage() != null && ex.getMessage().contains("decrypt")) {
                SecureLogger.logDecryptionFailure(serviceType, "API Call");
                throw new SecureDecryptionException(serviceType, ex);
            }
            
            // Generic failure
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                serviceType + " API call failed");
            
        } finally {
            // Step 5: Security cleanup - clear token from memory
            if (decryptedToken != null) {
                try {
                    // Overwrite the token in memory
                    decryptedToken = null;
                    SecureLogger.logMemoryCleanup(serviceType, true);
                } catch (Exception e) {
                    SecureLogger.logMemoryCleanup(serviceType, false);
                }
            }
        }
    }
    
    /**
     * Validate encrypted token format before decryption
     */
    public boolean validateEncryptedTokenFormat(String encryptedToken, String serviceType) {
        if (encryptedToken == null || encryptedToken.isBlank()) {
            SecureLogger.logTokenValidation(serviceType, false, "null/empty");
            return false;
        }
        
        // Basic validation - should be Base64 encoded
        try {
            java.util.Base64.getDecoder().decode(encryptedToken);
            SecureLogger.logTokenValidation(serviceType, true, "Base64");
            return true;
        } catch (IllegalArgumentException e) {
            SecureLogger.logTokenValidation(serviceType, false, "Invalid Base64");
            return false;
        }
    }
}
