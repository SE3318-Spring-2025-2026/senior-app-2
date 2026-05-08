package com.seniorapp.service;

import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.SprintDeliverableContribution;
import com.seniorapp.repository.ProjectSprintRepository;
import com.seniorapp.repository.SprintDeliverableContributionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Coordinator-facing service for managing Sprint → Deliverable contribution mappings.
 *
 * <p>Business rules:
 * <ul>
 *   <li>contributionPct must be in [1, 100].</li>
 *   <li>A sprint + deliverableName combination is unique (upsert semantics).</li>
 *   <li>Coordinator can delete a mapping if it was set in error.</li>
 * </ul>
 */
@Service
public class SprintDeliverableContributionService {

    private final SprintDeliverableContributionRepository contributionRepository;
    private final ProjectSprintRepository sprintRepository;

    public SprintDeliverableContributionService(
            SprintDeliverableContributionRepository contributionRepository,
            ProjectSprintRepository sprintRepository) {
        this.contributionRepository = contributionRepository;
        this.sprintRepository = sprintRepository;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Upserts a contribution mapping for (sprint, deliverableName).
     *
     * @param sprintId        ID of the sprint
     * @param deliverableName canonical name, e.g. "PROPOSAL", "SOW", "DEMONSTRATION"
     * @param contributionPct percentage [1, 100]
     * @param coordinatorId   user ID of the coordinator performing the action
     * @return saved entity
     */
    @Transactional
    public SprintDeliverableContribution upsert(Long sprintId,
                                                 String deliverableName,
                                                 Integer contributionPct,
                                                 Long coordinatorId) {
        validateContributionPct(contributionPct);
        validateDeliverableName(deliverableName);
        String normalizedName = deliverableName.toUpperCase().strip();

        ProjectSprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sprint not found: " + sprintId));

        SprintDeliverableContribution record =
                contributionRepository.findBySprintIdAndDeliverableName(sprintId, normalizedName)
                        .orElseGet(SprintDeliverableContribution::new);

        record.setSprint(sprint);
        record.setDeliverableName(normalizedName);
        record.setContributionPct(contributionPct);
        record.setSetByUserId(coordinatorId);

        return contributionRepository.save(record);
    }

    /** Returns all contribution mappings for a project, ordered by sprint then deliverable. */
    public List<SprintDeliverableContribution> getByProject(Long projectId) {
        return contributionRepository.findByProjectId(projectId);
    }

    /** Returns all contribution mappings for a specific sprint. */
    public List<SprintDeliverableContribution> getBySprint(Long sprintId) {
        return contributionRepository.findBySprintId(sprintId);
    }

    /**
     * Returns all sprint contributions for a deliverable within a project,
     * used by the grading engine to compute the deliverable scalar.
     */
    public List<SprintDeliverableContribution> getByProjectAndDeliverable(Long projectId, String deliverableName) {
        return contributionRepository.findByProjectIdAndDeliverableName(projectId, deliverableName.toUpperCase().strip());
    }

    /** Deletes a specific sprint → deliverable mapping. */
    @Transactional
    public void delete(Long sprintId, String deliverableName) {
        contributionRepository.deleteBySprintIdAndDeliverableName(sprintId,
                deliverableName.toUpperCase().strip());
    }

    // ── Validators ─────────────────────────────────────────────────────────────

    private void validateContributionPct(Integer pct) {
        if (pct == null || pct < 1 || pct > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "contributionPct must be between 1 and 100, got: " + pct);
        }
    }

    private void validateDeliverableName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "deliverableName must not be blank");
        }
    }
}
