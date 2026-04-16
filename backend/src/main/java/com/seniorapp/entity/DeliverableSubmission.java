package com.seniorapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "deliverable_submissions")
public class DeliverableSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
