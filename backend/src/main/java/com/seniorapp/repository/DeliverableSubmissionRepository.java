package com.seniorapp.repository;

import com.seniorapp.entity.DeliverableSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliverableSubmissionRepository extends JpaRepository<DeliverableSubmission, Long> {

    @Query(
            "select distinct s from DeliverableSubmission s join fetch s.deliverable d join fetch d.sprint sp "
                    + "join fetch sp.project p where s.id = :id")
    Optional<DeliverableSubmission> findByIdWithProjectChain(@Param("id") Long id);

    Optional<DeliverableSubmission> findByDeliverableIdAndGroupId(Long deliverableId, Long groupId);

    List<DeliverableSubmission> findByDeliverable_Sprint_Project_IdAndGroupId(Long projectId, Long groupId);

    List<DeliverableSubmission> findByGroupId(Long groupId);

    boolean existsByDeliverableIdAndGroupId(Long deliverableId, Long groupId);
}
