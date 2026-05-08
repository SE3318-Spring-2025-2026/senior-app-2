package com.seniorapp.service;

import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.SprintAdvisorGrade;
import com.seniorapp.entity.SprintGradeType;
import com.seniorapp.repository.ProjectSprintRepository;
import com.seniorapp.repository.SprintAdvisorGradeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Advisor-facing service for submitting and retrieving per-sprint soft-grades.
 *
 * <p>Soft-grade mapping (per spec): A=100, B=80, C=60, D=50, F=0.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Only letters A, B, C, D, F are valid.</li>
 *   <li>Submitting the same (sprint, group, gradeType) again updates the existing record.</li>
 *   <li>Advisors must belong to the group's project; enforcement is done at the controller layer.</li>
 * </ul>
 */
@Service
public class SprintAdvisorGradeService {

    /** Numeric values for each valid soft-grade letter (per specification). */
    public static final Map<String, Double> SOFT_GRADE_VALUES = Map.of(
            "A", 100.0,
            "B", 80.0,
            "C", 60.0,
            "D", 50.0,
            "F", 0.0
    );

    private static final Set<String> VALID_GRADES = SOFT_GRADE_VALUES.keySet();

    private final SprintAdvisorGradeRepository gradeRepository;
    private final ProjectSprintRepository sprintRepository;

    public SprintAdvisorGradeService(SprintAdvisorGradeRepository gradeRepository,
                                      ProjectSprintRepository sprintRepository) {
        this.gradeRepository = gradeRepository;
        this.sprintRepository = sprintRepository;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Upserts (create or update) an advisor grade for a sprint + group + type.
     *
     * @param sprintId      ID of the sprint
     * @param groupId       ID of the group being graded
     * @param advisorUserId ID of the advisor submitting the grade
     * @param gradeType     SCRUM (Point A) or CODE_REVIEW (Point B)
     * @param softGrade     letter grade: A, B, C, D, or F
     * @param comment       optional narrative comment (may be null)
     * @return saved entity
     */
    @Transactional
    public SprintAdvisorGrade upsert(Long sprintId,
                                      Long groupId,
                                      Long advisorUserId,
                                      SprintGradeType gradeType,
                                      String softGrade,
                                      String comment) {
        validateSoftGrade(softGrade);

        ProjectSprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sprint not found: " + sprintId));

        SprintAdvisorGrade record =
                gradeRepository.findBySprintIdAndGroupIdAndGradeType(sprintId, groupId, gradeType)
                        .orElseGet(SprintAdvisorGrade::new);

        record.setSprint(sprint);
        record.setGroupId(groupId);
        record.setAdvisorUserId(advisorUserId);
        record.setGradeType(gradeType);
        record.setSoftGrade(softGrade.toUpperCase().strip());
        record.setComment(comment);

        return gradeRepository.save(record);
    }

    /** Returns the numeric value (0–100) of a soft-grade letter. */
    public double toNumeric(String softGrade) {
        validateSoftGrade(softGrade);
        return SOFT_GRADE_VALUES.get(softGrade.toUpperCase().strip());
    }

    /**
     * Computes the deliverable scalar for a group from a list of soft-grade letters.
     *
     * <p>Formula: AVG(point-A-numeric, point-B-numeric, ...) / 100.0
     *
     * @param softGrades list of letter grades (must not be empty)
     * @return scalar value in [0.0, 1.0]
     */
    public double computeScalar(List<String> softGrades) {
        if (softGrades == null || softGrades.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "softGrades list must not be empty");
        }
        double sum = softGrades.stream()
                .mapToDouble(g -> SOFT_GRADE_VALUES.getOrDefault(g.toUpperCase().strip(), 0.0))
                .sum();
        return sum / softGrades.size() / 100.0;
    }

    /** All grades for a group within a sprint. */
    public List<SprintAdvisorGrade> getBySprintAndGroup(Long sprintId, Long groupId) {
        return gradeRepository.findBySprintIdAndGroupId(sprintId, groupId);
    }

    /** All grades for a group across all sprints in a project. */
    public List<SprintAdvisorGrade> getByProjectAndGroup(Long projectId, Long groupId) {
        return gradeRepository.findByProjectIdAndGroupId(projectId, groupId);
    }

    // ── Validators ─────────────────────────────────────────────────────────────

    private void validateSoftGrade(String softGrade) {
        if (softGrade == null || softGrade.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "softGrade must not be blank");
        }
        if (!VALID_GRADES.contains(softGrade.toUpperCase().strip())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "softGrade must be one of A, B, C, D, F – got: " + softGrade);
        }
    }
}
