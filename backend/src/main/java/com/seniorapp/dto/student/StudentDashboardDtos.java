package com.seniorapp.dto.student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class StudentDashboardDtos {
    private StudentDashboardDtos() {}

    public static class DashboardResponse {
        private String status;
        private DashboardData data;

        public DashboardResponse(String status, DashboardData data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() { return status; }
        public DashboardData getData() { return data; }
    }

    public static class DashboardData {
        private List<ActiveProjectItem> activeProjects;
        private List<PendingDeliverableItem> pendingDeliverables;
        private List<InviteItem> invitations;

        public List<ActiveProjectItem> getActiveProjects() { return activeProjects; }
        public void setActiveProjects(List<ActiveProjectItem> activeProjects) { this.activeProjects = activeProjects; }
        public List<PendingDeliverableItem> getPendingDeliverables() { return pendingDeliverables; }
        public void setPendingDeliverables(List<PendingDeliverableItem> pendingDeliverables) { this.pendingDeliverables = pendingDeliverables; }
        public List<InviteItem> getInvitations() { return invitations; }
        public void setInvitations(List<InviteItem> invitations) { this.invitations = invitations; }
    }

    public static class ActiveProjectItem {
        private Long projectId;
        private String projectTitle;
        private String term;
        private Long groupId;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getProjectTitle() { return projectTitle; }
        public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
        public String getTerm() { return term; }
        public void setTerm(String term) { this.term = term; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
    }

    public static class PendingDeliverableItem {
        private Long projectId;
        private String projectTitle;
        private String sprintTitle;
        private String deliverableTitle;
        private LocalDate dueDate;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getProjectTitle() { return projectTitle; }
        public void setProjectTitle(String projectTitle) { this.projectTitle = projectTitle; }
        public String getSprintTitle() { return sprintTitle; }
        public void setSprintTitle(String sprintTitle) { this.sprintTitle = sprintTitle; }
        public String getDeliverableTitle() { return deliverableTitle; }
        public void setDeliverableTitle(String deliverableTitle) { this.deliverableTitle = deliverableTitle; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    }

    public static class InviteItem {
        private Long inviteId;
        private Long groupId;
        private String groupName;
        private Long invitedByUserId;
        private LocalDateTime invitedAt;

        public Long getInviteId() { return inviteId; }
        public void setInviteId(Long inviteId) { this.inviteId = inviteId; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public Long getInvitedByUserId() { return invitedByUserId; }
        public void setInvitedByUserId(Long invitedByUserId) { this.invitedByUserId = invitedByUserId; }
        public LocalDateTime getInvitedAt() { return invitedAt; }
        public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
    }
}
