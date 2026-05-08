package com.seniorapp.service;

import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.repository.ProjectTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Aggressive unit tests for {@link AdvisorDeadlineService}.
 *
 * Edge cases:
 * - No active template -> lenient (no exception)
 * - Active template with no deadline -> lenient (no exception)
 * - Active template, deadline in future -> allowed
 * - Active template, deadline = today -> allowed (still today, not after)
 * - Active template, deadline = yesterday -> 403 FORBIDDEN
 * - Active template, deadline = far past -> 403 FORBIDDEN
 * - resolveActiveDeadline returns null when no template
 * - resolveActiveDeadline returns null when deadline not set
 * - resolveActiveDeadline returns date when set
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdvisorDeadlineService - Unit Tests")
class AdvisorDeadlineServiceTest {

    @Mock private ProjectTemplateRepository templateRepository;

    @InjectMocks
    private AdvisorDeadlineService service;

    // ══════════════════════════════════════════════════════════════════════════
    // assertAdvisorSelectionDeadlineNotPassed()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("assertAdvisorSelectionDeadlineNotPassed()")
    class AssertDeadlineTests {

        @Test
        @DisplayName("No active template -> lenient, no exception")
        void no_active_template_is_lenient() {
            when(templateRepository.findByActiveTrue()).thenReturn(List.of());
            assertDoesNotThrow(() -> service.assertAdvisorSelectionDeadlineNotPassed());
        }

        @Test
        @DisplayName("Active template but deadline is null -> lenient, no exception")
        void no_deadline_set_is_lenient() {
            ProjectTemplate t = templateWithDeadline(null);
            when(templateRepository.findByActiveTrue()).thenReturn(List.of(t));
            assertDoesNotThrow(() -> service.assertAdvisorSelectionDeadlineNotPassed());
        }

        @Test
        @DisplayName("Deadline in future -> allowed, no exception")
        void deadline_in_future_is_allowed() {
            ProjectTemplate t = templateWithDeadline(LocalDate.now().plusDays(10));
            when(templateRepository.findByActiveTrue()).thenReturn(List.of(t));
            assertDoesNotThrow(() -> service.assertAdvisorSelectionDeadlineNotPassed());
        }

        @Test
        @DisplayName("Deadline = today -> still allowed (isAfter, not isBefore)")
        void deadline_today_is_allowed() {
            ProjectTemplate t = templateWithDeadline(LocalDate.now());
            when(templateRepository.findByActiveTrue()).thenReturn(List.of(t));
            assertDoesNotThrow(() -> service.assertAdvisorSelectionDeadlineNotPassed());
        }

        @Test
        @DisplayName("Deadline = yesterday -> 403 FORBIDDEN")
        void deadline_yesterday_throws_403() {
            ProjectTemplate t = templateWithDeadline(LocalDate.now().minusDays(1));
            when(templateRepository.findByActiveTrue()).thenReturn(List.of(t));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.assertAdvisorSelectionDeadlineNotPassed());
            assertThat(ex.getStatusCode().value()).isEqualTo(403);
            assertThat(ex.getMessage()).containsIgnoringCase("deadline");
        }

        @Test
        @DisplayName("Deadline = 30 days ago -> 403 FORBIDDEN")
        void deadline_far_past_throws_403() {
            ProjectTemplate t = templateWithDeadline(LocalDate.now().minusDays(30));
            when(templateRepository.findByActiveTrue()).thenReturn(List.of(t));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.assertAdvisorSelectionDeadlineNotPassed());
            assertThat(ex.getStatusCode().value()).isEqualTo(403);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // resolveActiveDeadline()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveActiveDeadline()")
    class ResolveDeadlineTests {

        @Test
        @DisplayName("No active template -> returns null")
        void no_template_returns_null() {
            when(templateRepository.findByActiveTrue()).thenReturn(List.of());
            assertThat(service.resolveActiveDeadline()).isNull();
        }

        @Test
        @DisplayName("Active template with null deadline -> returns null")
        void null_deadline_returns_null() {
            when(templateRepository.findByActiveTrue()).thenReturn(List.of(templateWithDeadline(null)));
            assertThat(service.resolveActiveDeadline()).isNull();
        }

        @Test
        @DisplayName("Active template with deadline set -> returns that date")
        void returns_configured_deadline() {
            LocalDate date = LocalDate.of(2025, 10, 15);
            when(templateRepository.findByActiveTrue()).thenReturn(List.of(templateWithDeadline(date)));
            assertThat(service.resolveActiveDeadline()).isEqualTo(date);
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private ProjectTemplate templateWithDeadline(LocalDate deadline) {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(1L);
        t.setName("Test Template");
        t.setDescription("desc");
        t.setTerm("2025-Spring");
        t.setCreatedBy("admin");
        t.setCreatedByUserId(1L);
        t.setTemplateJson("{}");
        t.setAdvisorSelectionDeadline(deadline);
        return t;
    }
}
