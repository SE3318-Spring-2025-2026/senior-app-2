package com.seniorapp.repository;

import com.seniorapp.entity.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {
    List<SubmissionComment> findBySubmissionIdOrderByCreatedAtAsc(Long submissionId);
}
