package com.seniorapp.service;

import com.seniorapp.dto.DeliverableStatusDto;
import com.seniorapp.dto.ProjectInspectionDto;
import com.seniorapp.entity.Project;
import com.seniorapp.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Page<ProjectInspectionDto> getProjectsByFilter(String term, Long committeeId, Long advisorId, Pageable pageable) {
        Specification<Project> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (term != null && !term.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("term"), term));
            }
            if (committeeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("committeeId"), committeeId));
            }
            if (advisorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("advisorId"), advisorId));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Project> projects = projectRepository.findAll(spec, pageable);
        return projects.map(p -> new ProjectInspectionDto(p.getId(), p.getName(), p.getTerm(), p.getCommitteeId(), p.getAdvisorId()));
    }

    public List<DeliverableStatusDto> getProjectDeliverableStatuses(Long projectId) {
        // Since actual statuses come from DeliverableSubmission logic (Issue #97)
        // this is a stubbed return for the frontend implementation.
        // Once Issue #97 merges, this should fetch actual submissions and their grades.
        List<DeliverableStatusDto> statuses = new ArrayList<>();
        statuses.add(new DeliverableStatusDto(1L, "PENDING", null));
        statuses.add(new DeliverableStatusDto(2L, "GRADED", 85.5));
        return statuses;
    }
}
