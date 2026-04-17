package com.seniorapp.repository;

import com.seniorapp.entity.TemplateCommitteeProfessor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TemplateCommitteeProfessorRepository extends JpaRepository<TemplateCommitteeProfessor, Long> {
    Optional<TemplateCommitteeProfessor> findByCommitteeIdAndProfessor_Id(Long committeeId, Long professorId);

    @Query("select distinct tcp.committee.template.id from TemplateCommitteeProfessor tcp where tcp.professor.id = :professorId")
    List<Long> findDistinctTemplateIdsByProfessorUserId(Long professorId);

    boolean existsByCommittee_Template_IdAndProfessor_Id(Long templateId, Long professorId);

    @Query("select distinct tcp.professor.id from TemplateCommitteeProfessor tcp where tcp.committee.template.id = :templateId")
    List<Long> findDistinctProfessorIdsByTemplateId(Long templateId);
}
