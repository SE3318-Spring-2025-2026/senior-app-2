package com.seniorapp.service;

import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.SubmissionComment;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.SubmissionCommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class SubmissionCommentService {

    private final SubmissionCommentRepository commentRepository;
    private final DeliverableSubmissionRepository submissionRepository;

    public SubmissionCommentService(SubmissionCommentRepository commentRepository, 
                                    DeliverableSubmissionRepository submissionRepository) {
        this.commentRepository = commentRepository;
        this.submissionRepository = submissionRepository;
    }

    public SubmissionComment addComment(Long submissionId, Long userId, String content) {
        DeliverableSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        SubmissionComment comment = new SubmissionComment();
        comment.setSubmission(submission);
        comment.setAuthorUserId(userId);
        comment.setContent(content);

        return commentRepository.save(comment);
    }

    public List<SubmissionComment> getComments(Long submissionId) {
        return commentRepository.findBySubmissionIdOrderByCreatedAtAsc(submissionId);
    }
}
