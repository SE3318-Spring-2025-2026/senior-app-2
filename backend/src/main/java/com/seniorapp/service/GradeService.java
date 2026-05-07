package com.seniorapp.service;

import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeService {
    private final SubmissionGradeRepository gradeRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public GradeService(SubmissionGradeRepository gradeRepository,
                        DeliverableSubmissionRepository submissionRepository,
                        UserRepository userRepository) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    public SubmissionGrade saveGrade(Long submissionId, GradeSubmitRequest request) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
        
        User grader = userRepository.findById(request.getGraderId())
                .orElseThrow(() -> new IllegalArgumentException("Grader not found with ID: " + request.getGraderId()));

        if (gradeRepository.existsBySubmissionIdAndGraderIdAndRubricId(submissionId, request.getGraderId(), request.getRubricId())) {
            throw new IllegalArgumentException("This grader has already graded this rubric criterion (" + request.getRubricId() + ") for this submission.");
        }

        SubmissionGrade grade = new SubmissionGrade();
        grade.setSubmission(submission);
        grade.setGrader(grader);
        grade.setRubricId(request.getRubricId());
        grade.setGrade(request.getGrade());
        
        return gradeRepository.save(grade);
    }

    public List<SubmissionGrade> getGradesForSubmission(Long submissionId) {
        return gradeRepository.findBySubmissionId(submissionId);
    }
}
