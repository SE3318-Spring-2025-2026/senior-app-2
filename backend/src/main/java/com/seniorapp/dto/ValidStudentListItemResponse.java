package com.seniorapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Whitelist row for coordinator UI (no lazy proxies; ISO date string for reliable JSON).
 */
public class ValidStudentListItemResponse {

    private final Long id;
    private final String studentId;
    private final boolean linked;
    private final Long linkedAccountId;
    /** ISO-8601 local date-time string, e.g. {@code 2026-04-05T14:30:00} */
    private final String addedDate;
    private final String addedBy;

    public ValidStudentListItemResponse(Long id,
                                        String studentId,
                                        boolean linked,
                                        Long linkedAccountId,
                                        String addedDate,
                                        String addedBy) {
        this.id = id;
        this.studentId = studentId;
        this.linked = linked;
        this.linkedAccountId = linkedAccountId;
        this.addedDate = addedDate;
        this.addedBy = addedBy;
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    @JsonProperty("linked")
    public boolean isLinked() {
        return linked;
    }

    public Long getLinkedAccountId() {
        return linkedAccountId;
    }

    public String getAddedDate() {
        return addedDate;
    }

    public String getAddedBy() {
        return addedBy;
    }
}
