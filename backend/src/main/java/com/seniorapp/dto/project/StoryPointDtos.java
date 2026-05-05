package com.seniorapp.dto.project;

import java.util.ArrayList;
import java.util.List;

public final class StoryPointDtos {

    private StoryPointDtos() {}

    public static class StudentStoryPointRowDto {
        private Long studentUserId;
        private String fullName;
        private String email;
        private String membershipRole;
        private Double storyPoints;

        public Long getStudentUserId() {
            return studentUserId;
        }

        public void setStudentUserId(Long studentUserId) {
            this.studentUserId = studentUserId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMembershipRole() {
            return membershipRole;
        }

        public void setMembershipRole(String membershipRole) {
            this.membershipRole = membershipRole;
        }

        public Double getStoryPoints() {
            return storyPoints;
        }

        public void setStoryPoints(Double storyPoints) {
            this.storyPoints = storyPoints;
        }
    }

    public static class StoryPointsPayload {
        private List<StudentStoryPointRowDto> rows = new ArrayList<>();
        private boolean editable;

        public List<StudentStoryPointRowDto> getRows() {
            return rows;
        }

        public void setRows(List<StudentStoryPointRowDto> rows) {
            this.rows = rows != null ? rows : new ArrayList<>();
        }

        public boolean isEditable() {
            return editable;
        }

        public void setEditable(boolean editable) {
            this.editable = editable;
        }
    }

    public static class StoryPointsListResponse {
        private String status;
        private StoryPointsPayload data;

        public StoryPointsListResponse() {}

        public StoryPointsListResponse(String status, StoryPointsPayload data) {
            this.status = status;
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public StoryPointsPayload getData() {
            return data;
        }

        public void setData(StoryPointsPayload data) {
            this.data = data;
        }
    }

    public static class StoryPointEntry {
        private Long studentUserId;
        private Double storyPoints;

        public Long getStudentUserId() {
            return studentUserId;
        }

        public void setStudentUserId(Long studentUserId) {
            this.studentUserId = studentUserId;
        }

        public Double getStoryPoints() {
            return storyPoints;
        }

        public void setStoryPoints(Double storyPoints) {
            this.storyPoints = storyPoints;
        }
    }

    public static class SaveStoryPointsRequest {
        private List<StoryPointEntry> entries = new ArrayList<>();

        public List<StoryPointEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<StoryPointEntry> entries) {
            this.entries = entries != null ? entries : new ArrayList<>();
        }
    }
}
