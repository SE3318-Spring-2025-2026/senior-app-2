package com.seniorapp.repository;

import com.seniorapp.entity.ProjectSprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectSprintRepository extends JpaRepository<ProjectSprint, Long> {

    List<ProjectSprint> findByProjectIdOrderBySprintNoAsc(Long projectId);
}
