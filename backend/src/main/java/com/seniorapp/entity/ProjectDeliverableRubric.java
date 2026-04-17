package com.seniorapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_deliverable_rubrics")
public class ProjectDeliverableRubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_deliverable_id", nullable = false)
    private ProjectDeliverable deliverable;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String criteriaType;

    @Column(nullable = false)
    private Integer displayOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectDeliverable getDeliverable() {
        return deliverable;
    }

    public void setDeliverable(ProjectDeliverable deliverable) {
        this.deliverable = deliverable;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCriteriaType() {
        return criteriaType;
    }

    public void setCriteriaType(String criteriaType) {
        this.criteriaType = criteriaType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
