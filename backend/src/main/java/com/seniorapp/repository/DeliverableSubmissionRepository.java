package com.seniorapp.repository;

import com.seniorapp.entity.DeliverableSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliverableSubmissionRepository extends JpaRepository<DeliverableSubmission, Long> {

    Optional<DeliverableSubmission> findByDeliverableIdAndGroupId(Long deliverableId, Long groupId);

    List<DeliverableSubmission> findByDeliverable_Sprint_Project_IdAndGroupId(Long projectId, Long groupId);

    List<DeliverableSubmission> findByGroupId(Long groupId);

    boolean existsByDeliverableIdAndGroupId(Long deliverableId, Long groupId);
}
