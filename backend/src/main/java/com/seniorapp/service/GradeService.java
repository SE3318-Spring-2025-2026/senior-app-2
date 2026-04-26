package com.seniorapp.service;

import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        
        // When first created, we can also set the original AI score if applicable
        grade.setOriginalAiScore(request.getGrade());
        
        return gradeRepository.save(grade);
    }

    
    @Transactional
    public SubmissionGrade overrideGrade(Long gradeId, Double newScore, String advisorName) {
        // 1. Fetch existing grade
        SubmissionGrade gradeRecord = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Grade record not found with ID: " + gradeId));

        // 2. Preserve original AI score if it hasn't been set yet
        if (gradeRecord.getOriginalAiScore() == null) {
            gradeRecord.setOriginalAiScore(gradeRecord.getGrade());
        }

        // 3. Store the manual adjustment
        gradeRecord.setAdvisorAdjustedScore(newScore);
        
        // 4. Update the main grade field
        gradeRecord.setGrade(newScore);

        // 5. Automatic Recalculation to 4.0 scale
        // Formula: (newScore / 100.0) * 4.0
        double calculatedGpa = (newScore / 100.0) * 4.0;
        gradeRecord.setFinalGrade40(calculatedGpa);

        // 6. Audit traceability
        gradeRecord.setLastModifiedBy(advisorName);
        gradeRecord.setLastModifiedAt(LocalDateTime.now());

        return gradeRepository.save(gradeRecord);
    }

    public List<SubmissionGrade> getGradesForSubmission(Long submissionId) {
        return gradeRepository.findBySubmissionId(submissionId);
    }
}