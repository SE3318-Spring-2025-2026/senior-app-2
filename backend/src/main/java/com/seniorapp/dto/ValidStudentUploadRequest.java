package com.seniorapp.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request body for {@code POST /api/coordinator/valid-students}.
 * List of student identifiers to pre-approve (typically numeric student IDs;
 * e-mail strings are also allowed). New rows are not linked to GitHub until
 * the student completes OAuth; the SPA parses CSV as student numbers only (first column).
 *
 * <p>Example JSON:</p>
 * <pre>{@code
 * {
 *   "studentIds": ["210101001", "210101002"]
 * }
 * }</pre>
 */
public class ValidStudentUploadRequest {

    /**
     * Non-empty list of student ID strings (or e-mails) to whitelist.
     */
    @NotEmpty(message = "studentIds list must not be empty")
    private List<String> studentIds;

    public ValidStudentUploadRequest() {}

    public List<String> getStudentIds() { return studentIds; }
    public void setStudentIds(List<String> studentIds) { this.studentIds = studentIds; }
}
