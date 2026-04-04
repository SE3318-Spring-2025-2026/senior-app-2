package com.seniorapp.dto;

import com.seniorapp.entity.User;
import com.seniorapp.entity.UserStatus;

/**
 * Read-only DTO returned by {@code GET /api/auth/me} and similar endpoints.
 * Contains only the fields that are safe to expose to the frontend.
 */
public class UserResponseDTO {

    /** Internal database ID of the user. */
    private Long id;

    /** GitHub login name (username). */
    private String githubUsername;

    /** Primary e-mail address. */
    private String email;

    /** Full display name (may be null for GitHub-only accounts). */
    private String fullName;

    /** Role assigned to the user (e.g. STUDENT, COORDINATOR). */
    private String role;

    /** Account lifecycle status (ACTIVE, PENDING, DISBANDED). */
    private String status;

    // -------------------------------------------------------
    // Constructors
    // -------------------------------------------------------

    public UserResponseDTO() {}

    /**
     * Convenience factory method that maps a {@link User} entity
     * to a {@link UserResponseDTO}.
     *
     * @param user the source entity
     * @return a populated DTO
     */
    public static UserResponseDTO from(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.id             = user.getId();
        dto.githubUsername = user.getGithubUsername();
        dto.email          = user.getEmail();
        dto.fullName       = user.getFullName();
        dto.role           = user.getRole() != null ? user.getRole().name() : null;
        dto.status         = user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name();
        return dto;
    }

    // -------------------------------------------------------
    // Getters
    // -------------------------------------------------------

    public Long getId()               { return id; }
    public String getGithubUsername() { return githubUsername; }
    public String getEmail()          { return email; }
    public String getFullName()       { return fullName; }
    public String getRole()           { return role; }
    public String getStatus()         { return status; }
}
