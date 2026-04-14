package com.seniorapp.repository;

import com.seniorapp.entity.ProjectCommittee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectCommitteeRepository extends JpaRepository<ProjectCommittee, Long> {
    List<ProjectCommittee> findByProjectIdOrderByIdAsc(Long projectId);
}
