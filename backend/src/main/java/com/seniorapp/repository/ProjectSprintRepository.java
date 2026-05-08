package com.seniorapp.repository;

import com.seniorapp.entity.ProjectSprint;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSprintRepository extends JpaRepository<ProjectSprint, Long> {
    List<ProjectSprint> findByEndDateLessThanEqual(LocalDate endDate);
}
