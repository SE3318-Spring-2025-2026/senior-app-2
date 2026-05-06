package com.seniorapp.service.grading;

import com.seniorapp.dto.project.ProjectGradingSummaryDto;
import com.seniorapp.dto.project.ProjectGradingSummaryDto.DeliverableGradingLineDto;
import com.seniorapp.dto.project.ProjectGradingSummaryDto.StudentStoryPointGradingLineDto;
import com.seniorapp.dto.project.ProjectGradingSummaryDto.SprintProcessLineDto;
import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectDeliverable;
import com.seniorapp.entity.ProjectEvaluation;
import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.ProjectStudentStoryPoint;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.EvaluationRubricGradeRepository;
import com.seniorapp.repository.ProjectStudentStoryPointRepository;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Integrates the narrative of <em>Project Definition V2</em> (grading section) with persisted
 * deliverable rubric grades and sprint evaluation rubric grades.
 *
 * <p><strong>Simplifications vs full PDF spreadsheet:</strong></p>
 * <ul>
 *   <li>No coordinator-defined sprint→deliverable percentage matrix in DB: each deliverable inherits the
 *       scalar computed from <strong>its owning sprint only</strong> (the sprint entity the deliverable is under).</li>
 *   <li>&quot;Scrum&quot; vs &quot;Work/Code review&quot; rows: evaluations whose title contains
 *       {@code review}, {@code code}, or {@code kod} (case-insensitive, {@link Locale#ROOT}) are treated as
 *       Point-B style; all other evaluations in that sprint are Point-A style. Averages use neutral 100 when a branch
 *       has no grades yet (so missing data does not zero the sprint).</li>
 *   <li>Manual per-student story points (advisor UI, {@code project_student_story_points}) feed
 *       {@link ProjectGradingSummaryDto#getStudentStoryPointLines()} and team sums; {@code individualAllowanceFactor}
 *       stays {@code 1.0} for backward compatibility.</li>
 *   <li>Soft/Binary letter tables from the PDF are documented in {@link com.seniorapp.service.ProjectTemplateService}
 *       for template authoring; this engine consumes numeric rubric scores already stored on grades.</li>
 * </ul>
 */
@Service
public class PdfGradingEngineService {

    private static final double NEUTRAL_BRANCH = 100.0;

    private final DeliverableSubmissionRepository submissionRepository;
    private final SubmissionGradeRepository submissionGradeRepository;
    private final EvaluationRubricGradeRepository evaluationRubricGradeRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final ProjectStudentStoryPointRepository projectStudentStoryPointRepository;

    public PdfGradingEngineService(
            DeliverableSubmissionRepository submissionRepository,
            SubmissionGradeRepository submissionGradeRepository,
            EvaluationRubricGradeRepository evaluationRubricGradeRepository,
            UserGroupMemberRepository userGroupMemberRepository,
            ProjectStudentStoryPointRepository projectStudentStoryPointRepository) {
        this.submissionRepository = submissionRepository;
        this.submissionGradeRepository = submissionGradeRepository;
        this.evaluationRubricGradeRepository = evaluationRubricGradeRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.projectStudentStoryPointRepository = projectStudentStoryPointRepository;
    }

    @Transactional(readOnly = true)
    public ProjectGradingSummaryDto buildSummary(Project project, Long groupId) {
        Objects.requireNonNull(project, "project");
        if (groupId == null || groupId <= 0) {
            return null;
        }

        List<DeliverableSubmission> submissions =
                submissionRepository.findByDeliverable_Sprint_Project_IdAndGroupId(project.getId(), groupId);
        Map<Long, DeliverableSubmission> submissionByDeliverable = new HashMap<>();
        for (DeliverableSubmission s : submissions) {
            if (s.getDeliverable() != null && s.getDeliverable().getId() != null) {
                submissionByDeliverable.put(s.getDeliverable().getId(), s);
            }
        }

        Map<Long, Double> scalarBySprintId = new HashMap<>();
        Map<Long, Double> processMidBySprintId = new HashMap<>();
        List<SprintProcessLineDto> sprintLines = new ArrayList<>();
        List<ProjectSprint> sprints = project.getSprints().stream()
                .sorted(Comparator.comparing(ProjectSprint::getSprintNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        for (ProjectSprint sprint : sprints) {
            SprintAgg agg = aggregateSprintEvaluations(groupId, sprint);
            double mid = (agg.scrumMean() + agg.reviewMean()) / 2.0;
            double scalar = Math.min(1.0, Math.max(0.0, mid / 100.0));
            if (sprint.getId() != null) {
                scalarBySprintId.put(sprint.getId(), scalar);
                processMidBySprintId.put(sprint.getId(), mid);
            }
            SprintProcessLineDto line = new SprintProcessLineDto();
            line.setSprintNo(sprint.getSprintNo());
            line.setScrumEvaluationAvg(agg.scrumHadData ? round1(agg.scrumMean()) : null);
            line.setReviewEvaluationAvg(agg.reviewHadData ? round1(agg.reviewMean()) : null);
            line.setProcessMidScore(round1(mid));
            line.setSprintScalar(round4(scalar));
            sprintLines.add(line);
        }

        List<DeliverableGradingLineDto> delLines = new ArrayList<>();
        double weightedScaledSum = 0.0;
        int weightTotal = 0;
        double weightedScalarSum = 0.0;
        List<Double> rawSuccessSamples = new ArrayList<>();

        for (ProjectSprint sprint : sprints) {
            for (ProjectDeliverable d : sprint.getDeliverables()) {
                if (d.getId() == null) {
                    continue;
                }
                int w = d.getWeight() != null ? d.getWeight() : 0;
                weightTotal += w;

                DeliverableSubmission sub = submissionByDeliverable.get(d.getId());
                Double raw = null;
                if (sub != null && sub.getId() != null) {
                    raw = submissionGradeRepository.averageGradeBySubmissionId(sub.getId());
                }

                Long sprintKey = sprint.getId();
                double scalar = sprintKey != null ? scalarBySprintId.getOrDefault(sprintKey, 1.0) : 1.0;
                Double scaled = null;
                if (raw != null) {
                    scaled = raw * scalar;
                    weightedScaledSum += scaled * w;
                    weightedScalarSum += scalar * w;
                    rawSuccessSamples.add(raw);
                } else if (w > 0) {
                    weightedScalarSum += scalar * w;
                }

                double midForLine =
                        sprint.getId() != null ? processMidBySprintId.getOrDefault(sprint.getId(), NEUTRAL_BRANCH) : NEUTRAL_BRANCH;

                DeliverableGradingLineDto row = new DeliverableGradingLineDto();
                row.setDeliverableId(d.getId());
                row.setTitle(d.getTitle());
                row.setWeight(w);
                row.setRawSuccessGrade(raw != null ? round1(raw) : null);
                row.setSprintProcessMidScore(round1(midForLine));
                row.setSprintScalar(round4(scalar));
                row.setScaledGrade(scaled != null ? round1(scaled) : null);
                delLines.add(row);
            }
        }

        Double cumulative = null;
        if (weightTotal > 0) {
            cumulative = round1(weightedScaledSum / weightTotal);
        }

        Double overallSuccess = null;
        if (!rawSuccessSamples.isEmpty()) {
            overallSuccess = round1(rawSuccessSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        }

        Double weightedMeanScalar = weightTotal > 0 ? round4(weightedScalarSum / weightTotal) : null;

        double allowance = 1.0;
        Double adjusted = cumulative != null ? round1(cumulative * allowance) : null;

        ProjectGradingSummaryDto dto = new ProjectGradingSummaryDto();
        dto.setEngineVersion("pdf-v2-integrated");
        String baseModel =
                "Sprint scalar = avg(Point-A evaluations, Point-B evaluations)/100 on that deliverable's sprint "
                        + "(A=all non-review titles, B=title contains review/code/kod). "
                        + "Scaled = rawSuccess*rubric avg * scalar. Cumulative = sum(weight*scaled)/sum(weight). "
                        + "individualAllowanceFactor stays 1.0; optional manual story points add per-student lines.";
        dto.setModelDescription(baseModel);
        dto.setSprintProcessLines(sprintLines);
        dto.setDeliverableLines(delLines);
        dto.setCumulativeTeamGrade(cumulative);
        dto.setOverallSuccessGrade(overallSuccess);
        dto.setIndividualAllowanceFactor(allowance);
        dto.setAdjustedIndividualGrade(adjusted);
        dto.setWeightedMeanSprintScalar(weightedMeanScalar);
        attachManualStoryPoints(project, groupId, dto, cumulative);
        if (dto.getManualStoryPointsEnteredCount() != null
                && dto.getManualStoryPointsEnteredCount() > 0
                && dto.getManualStoryPointsTeamSum() != null) {
            dto.setModelDescription(
                    baseModel
                            + " Manual SP: "
                            + dto.getManualStoryPointsEnteredCount()
                            + "/"
                            + dto.getManualStoryPointsStudentCount()
                            + " students with values; team sum="
                            + dto.getManualStoryPointsTeamSum()
                            + ". Suggested individual grade = cumulative × (student SP / team sum).");
        }
        return dto;
    }

    private void attachManualStoryPoints(
            Project project, Long groupId, ProjectGradingSummaryDto dto, Double cumulativeTeamGrade) {
        List<UserGroupMember> members =
                userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
        List<UserGroupMember> students =
                members.stream()
                        .filter(m -> m.getUser() != null && m.getUser().getRole() == Role.STUDENT)
                        .sorted(
                                Comparator.comparing(
                                        (UserGroupMember m) -> m.getUser().getFullName(),
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .collect(Collectors.toList());

        Map<Long, Double> spByStudent = new HashMap<>();
        for (ProjectStudentStoryPoint row : projectStudentStoryPointRepository.findByProject_Id(project.getId())) {
            if (row.getStudentUserId() != null) {
                spByStudent.put(row.getStudentUserId(), row.getStoryPoints());
            }
        }

        double sum = 0.0;
        int entered = 0;
        for (UserGroupMember m : students) {
            Double sp = spByStudent.get(m.getUser().getId());
            if (sp != null) {
                sum += sp;
                entered++;
            }
        }

        dto.setManualStoryPointsStudentCount(students.size());
        dto.setManualStoryPointsEnteredCount(entered);
        dto.setManualStoryPointsTeamSum(entered > 0 ? round1(sum) : null);

        List<StudentStoryPointGradingLineDto> lines = new ArrayList<>();
        for (UserGroupMember m : students) {
            User u = m.getUser();
            StudentStoryPointGradingLineDto line = new StudentStoryPointGradingLineDto();
            line.setStudentUserId(u.getId());
            line.setFullName(u.getFullName());
            Double sp = spByStudent.get(u.getId());
            line.setManualStoryPoints(sp);
            if (sp != null && sum > 0) {
                double share = sp / sum;
                line.setStoryPointShareOfTeam(round4(share));
                if (cumulativeTeamGrade != null) {
                    line.setSuggestedIndividualGrade(round1(cumulativeTeamGrade * share));
                }
            }
            lines.add(line);
        }
        dto.setStudentStoryPointLines(lines);
    }

    private SprintAgg aggregateSprintEvaluations(Long groupId, ProjectSprint sprint) {
        List<Double> scrumScores = new ArrayList<>();
        List<Double> reviewScores = new ArrayList<>();
        for (ProjectEvaluation ev : sprint.getEvaluations()) {
            if (ev.getId() == null) {
                continue;
            }
            Double avg = evaluationRubricGradeRepository.averageGradeForGroupAndEvaluation(groupId, ev.getId());
            if (avg == null) {
                continue;
            }
            if (isReviewLike(ev.getTitle())) {
                reviewScores.add(avg);
            } else {
                scrumScores.add(avg);
            }
        }
        return new SprintAgg(scrumScores, reviewScores);
    }

    private static boolean isReviewLike(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        String t = title.toLowerCase(Locale.ROOT);
        return t.contains("review") || t.contains("code") || t.contains("kod");
    }

    private static Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static Double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static final class SprintAgg {
        private final boolean scrumHadData;
        private final boolean reviewHadData;
        private final double scrumMean;
        private final double reviewMean;

        SprintAgg(List<Double> scrum, List<Double> review) {
            this.scrumHadData = !scrum.isEmpty();
            this.reviewHadData = !review.isEmpty();
            this.scrumMean = scrumHadData ? average(scrum) : NEUTRAL_BRANCH;
            this.reviewMean = reviewHadData ? average(review) : NEUTRAL_BRANCH;
        }

        double scrumMean() {
            return scrumMean;
        }

        double reviewMean() {
            return reviewMean;
        }
    }

    private static double average(List<Double> xs) {
        return xs.stream().mapToDouble(Double::doubleValue).average().orElse(NEUTRAL_BRANCH);
    }
}
