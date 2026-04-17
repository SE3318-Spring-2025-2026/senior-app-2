package com.seniorapp.repository;

import com.seniorapp.entity.DeliverableSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliverableSubmissionRepository extends JpaRepository<DeliverableSubmission, Long> {
}
