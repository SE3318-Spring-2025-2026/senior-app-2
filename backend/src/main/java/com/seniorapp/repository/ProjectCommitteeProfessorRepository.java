package com.seniorapp.repository;

import com.seniorapp.entity.ProjectCommitteeProfessor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectCommitteeProfessorRepository extends JpaRepository<ProjectCommitteeProfessor, Long> {
    Optional<ProjectCommitteeProfessor> findByCommitteeIdAndProfessor_Id(Long committeeId, Long professorId);
}
