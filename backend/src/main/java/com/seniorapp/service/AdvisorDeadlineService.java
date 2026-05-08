package com.seniorapp.service;

import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.repository.ProjectTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Enforces schedule-based deadlines for the advisor selection process.
 *
 * <p>The active {@link ProjectTemplate} carries an {@code advisorSelectionDeadline}.
 * After this date, new advisee requests must be rejected (HTTP 403).
 *
 * <p>This service is called by any component that creates or modifies advisor requests,
 * including {@code GroupService} (team-leader advisee request) and the advisor flow.
 */
@Service
public class AdvisorDeadlineService {

    private final ProjectTemplateRepository templateRepository;

    public AdvisorDeadlineService(ProjectTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Throws {@code 403 FORBIDDEN} if the advisor selection deadline has passed.
     * If no active template is found, or the template has no deadline set, the
     * check is skipped (lenient fallback).
     */
    public void assertAdvisorSelectionDeadlineNotPassed() {
        LocalDate deadline = resolveActiveDeadline();
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Advisor selection deadline has passed (" + deadline + "). No new advisor requests allowed.");
        }
    }

    /**
     * Returns the deadline from the first active template, or {@code null} if
     * no active template exists or the template has no deadline configured.
     */
    public LocalDate resolveActiveDeadline() {
        List<ProjectTemplate> active = templateRepository.findByActiveTrue();
        if (active.isEmpty()) return null;
        return active.get(0).getAdvisorSelectionDeadline();
    }
}
