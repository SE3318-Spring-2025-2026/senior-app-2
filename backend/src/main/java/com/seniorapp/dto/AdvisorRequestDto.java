package com.seniorapp.dto;

public class AdvisorRequestDto {
    private String groupId;
    private String professorId;

    public AdvisorRequestDto() {}

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getProfessorId() { return professorId; }
    public void setProfessorId(String professorId) { this.professorId = professorId; }
}