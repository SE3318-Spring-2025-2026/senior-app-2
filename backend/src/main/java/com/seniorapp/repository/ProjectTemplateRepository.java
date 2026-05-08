package com.seniorapp.repository;

import com.seniorapp.entity.ProjectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplate, Long> {

    /** Returns all templates that are currently marked active (active = true). */
    List<ProjectTemplate> findByActiveTrue();
}
