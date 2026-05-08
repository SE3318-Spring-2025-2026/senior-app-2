package com.seniorapp.repository;

import com.seniorapp.entity.ProjectCommitteeProfessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectCommitteeProfessorRepository extends JpaRepository<ProjectCommitteeProfessor, Long> {
    Optional<ProjectCommitteeProfessor> findByCommitteeIdAndProfessor_Id(Long committeeId, Long professorId);

    boolean existsByCommittee_Project_IdAndProfessor_Id(Long projectId, Long professorUserId);

    @Query("""
            select p
            from ProjectCommitteeProfessor p
            where p.committee.project.id = :projectId
              and p.professor.id = :professorUserId
            order by p.committee.id asc
            """)
    List<ProjectCommitteeProfessor> findByProjectIdAndProfessorId(Long projectId, Long professorUserId);

    @Query("""
            select distinct p.professor.id
            from ProjectCommitteeProfessor p
            where p.committee.project.id = :projectId
            """)
    List<Long> findDistinctProfessorIdsByProjectId(Long projectId);
}
