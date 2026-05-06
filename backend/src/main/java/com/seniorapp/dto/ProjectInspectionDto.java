package com.seniorapp.dto;

public class ProjectInspectionDto {
    private Long id;
    private String name;
    private String term;
    private Long committeeId;
    private Long advisorId;

    public ProjectInspectionDto(Long id, String name, String term, Long committeeId, Long advisorId) {
        this.id = id;
        this.name = name;
        this.term = term;
        this.committeeId = committeeId;
        this.advisorId = advisorId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getTerm() { return term; }
    public Long getCommitteeId() { return committeeId; }
    public Long getAdvisorId() { return advisorId; }
}
