package com.seniorapp.repository;

import com.seniorapp.entity.DeliverableSubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliverableSubmissionFileRepository extends JpaRepository<DeliverableSubmissionFile, Long> {

    List<DeliverableSubmissionFile> findBySubmission_IdOrderByIdDesc(Long submissionId);
}
