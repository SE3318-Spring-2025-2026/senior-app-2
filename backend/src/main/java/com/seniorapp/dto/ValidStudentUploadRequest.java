package com.seniorapp.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request body for {@code POST /api/coordinator/valid-students}.
 * Contains a list of student e-mail addresses (or ID strings) that the
 * Coordinator wants to pre-approve for GitHub-based account creation.
 *
 * <p>Example JSON:</p>
 * <pre>{@code
 * {
 *   "studentIds": ["std001@uni.edu", "std002@uni.edu"]
 * }
 * }</pre>
 */
public class ValidStudentUploadRequest {

    /**
     * Non-empty list of student e-mail addresses or student ID strings
     * to add to the whitelist.
     */
    @NotEmpty(message = "studentIds list must not be empty")
    private List<String> studentIds;

    public ValidStudentUploadRequest() {}

    public List<String> getStudentIds() { return studentIds; }
    public void setStudentIds(List<String> studentIds) { this.studentIds = studentIds; }
}
