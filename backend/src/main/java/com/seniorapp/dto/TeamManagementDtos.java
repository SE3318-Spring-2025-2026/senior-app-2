package com.seniorapp.dto;

import java.util.List;

public final class TeamManagementDtos {
    private TeamManagementDtos() {}

    public static class TeamListResponse {
        private String status;
        private List<TeamDto> data;

        public TeamListResponse(String status, List<TeamDto> data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() { return status; }
        public List<TeamDto> getData() { return data; }
    }

    public static class TeamDto {
        private Long groupId;
        private String groupName;
        private Long leaderUserId;
        private boolean currentUserLeader;
        private ProjectLinkDto project;
        private List<MemberDto> members;

        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public Long getLeaderUserId() { return leaderUserId; }
        public void setLeaderUserId(Long leaderUserId) { this.leaderUserId = leaderUserId; }
        public boolean isCurrentUserLeader() { return currentUserLeader; }
        public void setCurrentUserLeader(boolean currentUserLeader) { this.currentUserLeader = currentUserLeader; }
        public ProjectLinkDto getProject() { return project; }
        public void setProject(ProjectLinkDto project) { this.project = project; }
        public List<MemberDto> getMembers() { return members; }
        public void setMembers(List<MemberDto> members) { this.members = members; }
    }

    public static class ProjectLinkDto {
        private Long projectId;
        private String title;
        private String term;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getTerm() { return term; }
        public void setTerm(String term) { this.term = term; }
    }

    public static class MemberDto {
        private Long userId;
        private String fullName;
        private String email;
        private String role;
        private String inviteStatus;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getInviteStatus() { return inviteStatus; }
        public void setInviteStatus(String inviteStatus) { this.inviteStatus = inviteStatus; }
    }

    public static class StudentOptionDto {
        private Long userId;
        private String fullName;
        private String email;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class StudentListResponse {
        private String status;
        private List<StudentOptionDto> data;

        public StudentListResponse(String status, List<StudentOptionDto> data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() { return status; }
        public List<StudentOptionDto> getData() { return data; }
    }

    public static class CreateProjectFromTemplateRequest {
        private Long templateId;

        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
    }
}
