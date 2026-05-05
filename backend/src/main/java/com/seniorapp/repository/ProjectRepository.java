package com.seniorapp.repository;

import com.seniorapp.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Proje yüklemesi {@code findById} ile yapılır; nested koleksiyonlar aynı transaction içinde
 * {@link com.seniorapp.service.ProjectService#warmProjectSprintCollections} ile açılır.
 * Özel {@code join fetch} / derin {@code EntityGraph} Hibernate 6.x’te “Could not generate fetch” tetikleyebildi.
 */
public interface ProjectRepository extends JpaRepository<Project, Long> {
}
