package com.seniorapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_evaluation_rubrics")
public class ProjectEvaluationRubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_evaluation_id", nullable = false)
    private ProjectEvaluation evaluation;

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

    public ProjectEvaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(ProjectEvaluation evaluation) {
        this.evaluation = evaluation;
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
