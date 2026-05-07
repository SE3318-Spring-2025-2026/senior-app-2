package com.seniorapp.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_evaluations")
public class ProjectEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_sprint_id", nullable = false)
    private ProjectSprint sprint;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Integer weight;

    @Column(nullable = false)
    private boolean autoAddToAllSprints;

    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectEvaluationRubric> rubrics = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectSprint getSprint() {
        return sprint;
    }

    public void setSprint(ProjectSprint sprint) {
        this.sprint = sprint;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public boolean isAutoAddToAllSprints() {
        return autoAddToAllSprints;
    }

    public void setAutoAddToAllSprints(boolean autoAddToAllSprints) {
        this.autoAddToAllSprints = autoAddToAllSprints;
    }

    public List<ProjectEvaluationRubric> getRubrics() {
        return rubrics;
    }

    public void setRubrics(List<ProjectEvaluationRubric> rubrics) {
        this.rubrics = rubrics;
    }
}
