package com.seniorapp.dto;

public class GroupMemberActionDto {
    private String studentId;
    private String action;
    private String leaderId;

    public GroupMemberActionDto() {}
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
}