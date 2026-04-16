package com.seniorapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String term;

    @Column(name = "committee_id")
    private Long committeeId;

    @Column(name = "advisor_id")
    private Long advisorId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public Long getCommitteeId() { return committeeId; }
    public void setCommitteeId(Long committeeId) { this.committeeId = committeeId; }

    public Long getAdvisorId() { return advisorId; }
    public void setAdvisorId(Long advisorId) { this.advisorId = advisorId; }
}
