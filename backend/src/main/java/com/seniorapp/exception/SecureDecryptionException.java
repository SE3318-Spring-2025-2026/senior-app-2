package com.seniorapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Secure exception for token decryption failures in Process 5.1.
 * Never exposes sensitive data in error messages.
 */
public class SecureDecryptionException extends ResponseStatusException {
    
    private final String serviceType;
    
    public SecureDecryptionException(String serviceType) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, serviceType + " authentication failed");
        this.serviceType = serviceType;
    }
    
    public SecureDecryptionException(String serviceType, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, serviceType + " authentication failed", cause);
        this.serviceType = serviceType;
    }
    
    public String getServiceType() {
        return serviceType;
    }
    
    /**
     * Override to ensure no sensitive data is ever exposed in error messages
     */
    @Override
    public String getMessage() {
        return serviceType + " authentication failed";
    }
}
