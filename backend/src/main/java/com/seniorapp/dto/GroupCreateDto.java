package com.seniorapp.dto;

public class GroupCreateDto {
    private String studentId;
    private String groupName;
    private String projectId;

    public GroupCreateDto() {}
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
}