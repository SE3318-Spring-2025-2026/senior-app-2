package com.seniorapp.exception;

/**
 * Thrown when an AI validation request exceeds the allowed time budget.
 */
public class AiValidationTimeoutException extends RuntimeException {

    public AiValidationTimeoutException(String message) {
        super(message);
    }

    public AiValidationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

