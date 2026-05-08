package com.seniorapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Whitelist row for coordinator UI (no lazy proxies; ISO date string for reliable JSON).
 * Frontend tarafındaki filtreleme ve tablo görünümü için gerekli tüm alanları taşır.
 */
public class ValidStudentListItemResponse {

    private final Long id;
    private final String studentId;
    private final boolean linked;
    private final Long linkedAccountId;
    
    /** ISO-8601 formatında tarih: "2026-04-05T14:30:00" */
    private final String addedDate;
    private final String addedBy;

    public ValidStudentListItemResponse(
            Long id,
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

    // Frontend'deki isRowLinked kontrolü ve "linked" filtresi için önemli
    @JsonProperty("linked")
    public boolean isLinked() {
        return linked;
    }

    @JsonProperty("userId") // Frontend'deki navigate(`/panel/github-profile/${row.userId}`) kısmı için
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