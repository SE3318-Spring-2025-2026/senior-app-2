package com.seniorapp.dto;

public class GroupInviteRespondDto {
    private String action;
    private Long committeeId;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getCommitteeId() {
        return committeeId;
    }

    public void setCommitteeId(Long committeeId) {
        this.committeeId = committeeId;
    }
}
