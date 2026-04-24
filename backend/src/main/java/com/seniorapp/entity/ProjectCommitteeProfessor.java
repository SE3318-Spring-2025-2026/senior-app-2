package com.seniorapp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_committee_professors",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"committee_id", "professor_user_id"})
        }
)
public class ProjectCommitteeProfessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "committee_id", nullable = false)
    private ProjectCommittee committee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "professor_user_id", nullable = false)
    private User professor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectCommittee getCommittee() {
        return committee;
    }

    public void setCommittee(ProjectCommittee committee) {
        this.committee = committee;
    }

    public User getProfessor() {
        return professor;
    }

    public void setProfessor(User professor) {
        this.professor = professor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
