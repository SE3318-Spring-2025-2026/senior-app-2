package com.seniorapp.repository;

import com.seniorapp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for {@link Project} entities.
 * Provides lookup methods beyond the standard JPA operations.
 */
public interface ProjectRepository extends JpaRepository<Project, String> {
    Optional<Project> findByName(String name);
}
