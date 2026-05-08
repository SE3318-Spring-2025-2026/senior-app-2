package com.seniorapp.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "evaluation_rubric_grades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "evaluation_rubric_id", "grader_id"}))
public class EvaluationRubricGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evaluation_rubric_id", nullable = false)
    private ProjectEvaluationRubric evaluationRubric;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grader_id", nullable = false)
    private User grader;

    @Column(nullable = false)
    private Double grade;

    @Column(length = 2000)
    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public ProjectEvaluationRubric getEvaluationRubric() {
        return evaluationRubric;
    }

    public void setEvaluationRubric(ProjectEvaluationRubric evaluationRubric) {
        this.evaluationRubric = evaluationRubric;
    }

    public User getGrader() {
        return grader;
    }

    public void setGrader(User grader) {
        this.grader = grader;
    }

    public Double getGrade() {
        return grade;
    }

    public void setGrade(Double grade) {
        this.grade = grade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
