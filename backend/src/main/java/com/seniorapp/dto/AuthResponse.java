package com.seniorapp.dto;

public class AuthResponse {

    private String token;
    private UserInfo user;

    public AuthResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public UserInfo getUser() { return user; }

    public static class UserInfo {
        private Long id;
        private String email;
        private String fullName;
        private String role;
        private String githubUsername;

        public UserInfo(Long id, String email, String fullName, String role,
                        String githubUsername) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.githubUsername = githubUsername;
        }

        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public String getGithubUsername() { return githubUsername; }
    }
}
