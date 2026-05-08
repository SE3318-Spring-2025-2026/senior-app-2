package com.seniorapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Secure logging utility for Process 5.1 that never exposes sensitive data.
 * All token values, IV values, and plaintext credentials are masked.
 */
@Component
public class SecureLogger {

    private static final Logger log = LoggerFactory.getLogger(SecureLogger.class);

    /**
     * Log Process 5.1 operations without exposing sensitive data
     */
    public void logProcessStart(String serviceType, String operation, String endpoint) {
        log.info("Process 5.1: Starting {} operation - Type: {}, Endpoint: {}",
                operation, serviceType, sanitizeEndpoint(endpoint));
    }

    /**
     * Log successful Process 5.1 operations
     */
    public void logProcessSuccess(String serviceType, String operation, String statusCode) {
        log.info("Process 5.1: {} operation completed successfully - Type: {}, Status: {}",
                operation, serviceType, statusCode);
    }
    
    /**
     * Log Process 5.1 failures without exposing sensitive data
     */
    public void logProcessFailure(String serviceType, String operation, String errorType,
                                       String endpoint, String method) {
        log.error("Process 5.1: {} operation failed - Type: {}, Error: {}, Endpoint: {}, Method: {}",
                 operation, serviceType, errorType, sanitizeEndpoint(endpoint), method);
    }
    
    /**
     * Log decryption failures securely - never log tokens or IV values
     */
    public void logDecryptionFailure(String serviceType, String operation) {
        log.error("Process 5.1: Token decryption failed - Type: {}, Operation: {}", serviceType, operation);
    }
    
    /**
     * Log token validation results
     */
    public void logTokenValidation(String serviceType, boolean isValid, String tokenFormat) {
        if (isValid) {
            log.debug("Process 5.1: Token validation passed - Type: {}, Format: {}", serviceType, tokenFormat);
        } else {
            log.warn("Process 5.1: Token validation failed - Type: {}, Format: {}", serviceType, tokenFormat);
        }
    }
    
    /**
     * Log API call attempts without exposing authentication details
     */
    public void logApiCallAttempt(String serviceType, String method, String endpoint) {
        log.info("Process 5.1: API call initiated - Type: {}, Method: {}, Endpoint: {}",
                serviceType, method, sanitizeEndpoint(endpoint));
    }
    
    /**
     * Sanitize endpoint URLs by removing sensitive query parameters
     */
    private static String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) return "null";
        
        // Remove access tokens, API keys, and other sensitive parameters
        String sanitized = endpoint.replaceAll("[?&]access_token=[^&]*", "")
                                  .replaceAll("[?&]api_key=[^&]*", "")
                                  .replaceAll("[?&]token=[^&]*", "")
                                  .replaceAll("[?&]password=[^&]*", "")
                                  .replaceAll("[?&]secret=[^&]*", "");
        
        // Replace remaining sensitive parts with placeholder
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 97) + "...";
        }
        
        return sanitized;
    }
    
    /**
     * Mask token values for logging - shows only first and last 3 characters
     */
    public String maskToken(String token) {
        if (token == null || token.length() <= 6) {
            return "***";
        }
        return token.substring(0, 3) + "***" + token.substring(token.length() - 3);
    }
    
    /**
     * Log memory cleanup operations
     */
    public void logMemoryCleanup(String serviceType, boolean successful) {
        if (successful) {
            log.debug("Process 5.1: Memory cleanup completed - Type: {}", serviceType);
        } else {
            log.warn("Process 5.1: Memory cleanup failed - Type: {} (non-critical)", serviceType);
        }
    }
}
