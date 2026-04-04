package com.seniorapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a student's GitHub e-mail is not found in the
 * pre-approved {@code ValidStudentId} whitelist.
 *
 * <p>Maps automatically to HTTP 403 Forbidden via
 * {@link com.seniorapp.exception.GlobalExceptionHandler}.</p>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class StudentNotWhitelistedException extends RuntimeException {

    /**
     * @param email the e-mail address that was not found on the whitelist
     */
    public StudentNotWhitelistedException(String email) {
        super("Access denied: '" + email + "' is not on the approved student list. "
              + "Please contact your coordinator.");
    }
}
