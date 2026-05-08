package com.seniorapp.entity;

import jakarta.persistence.*;

/**
 * Maps what percentage a given sprint contributes to a named deliverable.
 *
 * <p>Example: "Sprint 1 contributes 70% to PROPOSAL, Sprint 1+2 contribute to SOW".
 * The coordinator configures these values through the coordinator API.
 *
 * <p>The combination (sprint, deliverableName) is unique per project.
 */
@Entity
@Table(
        name = "sprint_deliverable_contributions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sprint_id", "deliverable_name"})
)
public class SprintDeliverableContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private ProjectSprint sprint;

    /** Canonical deliverable name, e.g. "PROPOSAL", "SOW", "DEMONSTRATION". */
    @Column(name = "deliverable_name", nullable = false, length = 100)
    private String deliverableName;

    /**
     * Contribution percentage this sprint carries for the deliverable.
     * Must be in the range [1, 100].
     */
    @Column(name = "contribution_pct", nullable = false)
    private Integer contributionPct;

    /** User ID of the coordinator who set this mapping. */
    @Column(name = "set_by_user_id", nullable = false)
    private Long setByUserId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProjectSprint getSprint() { return sprint; }
    public void setSprint(ProjectSprint sprint) { this.sprint = sprint; }

    public String getDeliverableName() { return deliverableName; }
    public void setDeliverableName(String deliverableName) { this.deliverableName = deliverableName; }

    public Integer getContributionPct() { return contributionPct; }
    public void setContributionPct(Integer contributionPct) { this.contributionPct = contributionPct; }

    public Long getSetByUserId() { return setByUserId; }
    public void setSetByUserId(Long setByUserId) { this.setByUserId = setByUserId; }
}
