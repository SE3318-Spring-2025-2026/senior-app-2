package com.seniorapp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "template_committee_professors",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"committee_id", "professor_user_id"})
        }
)
public class TemplateCommitteeProfessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "committee_id", nullable = false)
    private TemplateCommittee committee;

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

    public TemplateCommittee getCommittee() {
        return committee;
    }

    public void setCommittee(TemplateCommittee committee) {
        this.committee = committee;
    }

    public User getProfessor() {
        return professor;
    }

    public void setProfessor(User professor) {
        this.professor = professor;
    }
}
