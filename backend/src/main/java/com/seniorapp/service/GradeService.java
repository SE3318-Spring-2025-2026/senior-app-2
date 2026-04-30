package com.seniorapp.service;

import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.ProjectCommitteeProfessorRepository;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeService {
    private final SubmissionGradeRepository gradeRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    private final ProjectCommitteeProfessorRepository projectCommitteeProfessorRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final LogService logService;

    public GradeService(SubmissionGradeRepository gradeRepository,
                        DeliverableSubmissionRepository submissionRepository,
                        ProjectGroupAssignmentRepository projectGroupAssignmentRepository,
                        ProjectCommitteeProfessorRepository projectCommitteeProfessorRepository,
                        UserGroupMemberRepository userGroupMemberRepository,
                        LogService logService) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.projectGroupAssignmentRepository = projectGroupAssignmentRepository;
        this.projectCommitteeProfessorRepository = projectCommitteeProfessorRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.logService = logService;
    }

    public SubmissionGrade saveGrade(Long submissionId, GradeSubmitRequest request) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));

        User currentUser = currentUser();
        if (!canGradeSubmission(currentUser, submission)) {
            logService.saveLog(
                    currentUser.getId(),
                    currentUser.getRole().name(),
                    "grading",
                    "grade_submit_denied",
                    "blocked",
                    "warning",
                    "User is not allowed to grade this submission."
            );
            throw new SecurityException("You are not allowed to grade this group.");
        }

        if (gradeRepository.existsBySubmissionIdAndGraderIdAndRubricId(submissionId, currentUser.getId(), request.getRubricId())) {
            throw new IllegalArgumentException("This grader has already graded this rubric criterion (" + request.getRubricId() + ") for this submission.");
        }

        SubmissionGrade grade = new SubmissionGrade();
        grade.setSubmission(submission);
        grade.setGrader(currentUser);
        grade.setRubricId(request.getRubricId());
        grade.setGrade(request.getGrade());

        SubmissionGrade saved = gradeRepository.save(grade);
        logService.saveLog(
                currentUser.getId(),
                currentUser.getRole().name(),
                "grading",
                "grade_submitted",
                "success",
                "info",
                "Submission " + submissionId + " graded for rubric " + request.getRubricId()
        );
        return saved;
    }

    public List<SubmissionGrade> getGradesForSubmission(Long submissionId) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
        User currentUser = currentUser();
        if (!canViewSubmission(currentUser, submission)) {
            throw new SecurityException("You are not allowed to view grades for this submission.");
        }
        return gradeRepository.findBySubmissionId(submissionId);
    }

    private User currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new SecurityException("Authenticated user not found.");
    }

    private boolean canViewSubmission(User user, DeliverableSubmission submission) {
        Role role = user.getRole();
        if (role == Role.ADMIN || role == Role.COORDINATOR) {
            return true;
        }
        Long projectId = submission.getDeliverable().getSprint().getProject().getId();
        if (role == Role.PROFESSOR) {
            return projectCommitteeProfessorRepository.existsByCommittee_Project_IdAndProfessor_Id(projectId, user.getId());
        }
        if (role == Role.STUDENT) {
            return userGroupMemberRepository
                    .findByGroupIdAndUserIdAndStatus(submission.getGroupId(), user.getId(), GroupInviteStatus.ACCEPTED)
                    .isPresent();
        }
        return false;
    }

    private boolean canGradeSubmission(User user, DeliverableSubmission submission) {
        Role role = user.getRole();
        if (role == Role.ADMIN) {
            return true;
        }
        if (role != Role.COORDINATOR && role != Role.PROFESSOR) {
            return false;
        }

        Long projectId = submission.getDeliverable().getSprint().getProject().getId();
        var assignment = projectGroupAssignmentRepository
                .findByProject_IdAndGroupIdAndActiveTrue(projectId, submission.getGroupId())
                .orElse(null);
        if (assignment == null) {
            return false;
        }

        if (assignment.getCommittee() == null) {
            return projectCommitteeProfessorRepository.existsByCommittee_Project_IdAndProfessor_Id(projectId, user.getId());
        }

        return projectCommitteeProfessorRepository
                .existsByCommittee_IdAndProfessor_Id(assignment.getCommittee().getId(), user.getId());
    }
}
