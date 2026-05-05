package com.seniorapp.repository;

import com.seniorapp.entity.ProjectDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectDeliverableRepository extends JpaRepository<ProjectDeliverable, Long> {

    @Query(
            "select distinct d from ProjectDeliverable d join fetch d.sprint s join fetch s.project p where d.id = :id")
    Optional<ProjectDeliverable> findByIdWithProjectChain(@Param("id") Long id);
}
