package com.seniorapp.dto;

/**
 * Result of checking a student ID against {@code valid_student_ids}.
 */
public record StudentIdValidityResponse(
        String studentId,
        boolean valid,
        boolean linked,
        Long matchedAccountId
) {}
