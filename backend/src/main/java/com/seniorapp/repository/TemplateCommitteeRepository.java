package com.seniorapp.repository;

import com.seniorapp.entity.TemplateCommittee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateCommitteeRepository extends JpaRepository<TemplateCommittee, Long> {
    List<TemplateCommittee> findByTemplateIdOrderByIdAsc(Long templateId);
}
