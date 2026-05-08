package com.seniorapp.service;

import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.SprintStudentStoryPointTarget;
import com.seniorapp.repository.ProjectSprintRepository;
import com.seniorapp.repository.SprintStudentStoryPointTargetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Coordinator-facing service for setting per-student per-sprint story point targets.
 *
 * <p>Business rules:
 * <ul>
 *   <li>targetPoints >= 0 (0 is valid; coordinator may set to 0 to temporarily exclude a student).</li>
 *   <li>Upsert semantics: setting target again updates the existing record.</li>
 * </ul>
 */
@Service
public class SprintStudentStoryPointTargetService {

    private final SprintStudentStoryPointTargetRepository targetRepository;
    private final ProjectSprintRepository sprintRepository;

    public SprintStudentStoryPointTargetService(
            SprintStudentStoryPointTargetRepository targetRepository,
            ProjectSprintRepository sprintRepository) {
        this.targetRepository = targetRepository;
        this.sprintRepository = sprintRepository;
    }

    /**
     * Upserts a story-point target for a student in a sprint.
     *
     * @param sprintId      ID of the sprint
     * @param studentUserId ID of the student
     * @param targetPoints  target story points (>= 0)
     * @param coordinatorId user ID of the coordinator
     * @return saved entity
     */
    @Transactional
    public SprintStudentStoryPointTarget upsert(Long sprintId,
                                                 Long studentUserId,
                                                 Integer targetPoints,
                                                 Long coordinatorId) {
        if (targetPoints == null || targetPoints < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "targetPoints must be >= 0, got: " + targetPoints);
        }

        ProjectSprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sprint not found: " + sprintId));

        SprintStudentStoryPointTarget record =
                targetRepository.findBySprintIdAndStudentUserId(sprintId, studentUserId)
                        .orElseGet(SprintStudentStoryPointTarget::new);

        record.setSprint(sprint);
        record.setStudentUserId(studentUserId);
        record.setTargetPoints(targetPoints);
        record.setSetByUserId(coordinatorId);

        return targetRepository.save(record);
    }

    /** Get target for a specific student in a sprint. */
    public SprintStudentStoryPointTarget getTarget(Long sprintId, Long studentUserId) {
        return targetRepository.findBySprintIdAndStudentUserId(sprintId, studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No target set for student " + studentUserId + " in sprint " + sprintId));
    }

    /** All targets for a sprint (all students in that sprint). */
    public List<SprintStudentStoryPointTarget> getBySprintId(Long sprintId) {
        return targetRepository.findBySprintId(sprintId);
    }

    /** All targets for a student across all sprints in a project. */
    public List<SprintStudentStoryPointTarget> getByProjectAndStudent(Long projectId, Long studentUserId) {
        return targetRepository.findByProjectIdAndStudentUserId(projectId, studentUserId);
    }
}
