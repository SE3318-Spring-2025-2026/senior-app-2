package com.seniorapp.repository;

import com.seniorapp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    java.util.Optional<Project> findByGroupId(Long groupId);
}
