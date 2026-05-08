package com.seniorapp.repository;

import com.seniorapp.entity.ProjectDeliverableRubric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectDeliverableRubricRepository extends JpaRepository<ProjectDeliverableRubric, Long> {

    List<ProjectDeliverableRubric> findByDeliverable_IdIn(Collection<Long> deliverableIds);
}
