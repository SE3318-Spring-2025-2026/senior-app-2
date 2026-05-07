package com.seniorapp.dto;

public class AdvisorDecisionDto {
    private String requestId;
    private String currentProfessorId;
    private String decision;

    public AdvisorDecisionDto() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getCurrentProfessorId() { return currentProfessorId; }
    public void setCurrentProfessorId(String currentProfessorId) { this.currentProfessorId = currentProfessorId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}