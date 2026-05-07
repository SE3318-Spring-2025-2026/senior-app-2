package com.seniorapp.repository;

import com.seniorapp.entity.ProjectDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectDeliverableRepository extends JpaRepository<ProjectDeliverable, Long> {
}
