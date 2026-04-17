package com.seniorapp.repository;

import com.seniorapp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

/**
 * Repository for {@link Project} entities.
 * Provides lookup methods beyond the standard JPA operations.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {
    /** Find a Project by the numeric GitHub account ID. */
    Optional<Project> findById(Long id);

    /** Find a Project by their GitHub login name (Projectname). */
    Optional<Project> findByName(String name);
}
