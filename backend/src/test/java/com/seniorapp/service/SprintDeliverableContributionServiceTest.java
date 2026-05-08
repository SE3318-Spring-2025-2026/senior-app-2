package com.seniorapp.service;

import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.SprintDeliverableContribution;
import com.seniorapp.repository.ProjectSprintRepository;
import com.seniorapp.repository.SprintDeliverableContributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Aggressive unit tests for {@link SprintDeliverableContributionService}.
 *
 * Edge cases:
 * - contributionPct = 0 → rejected
 * - contributionPct = 101 → rejected
 * - null pct → rejected
 * - null/blank deliverableName → rejected
 * - upsert creates new record when not found
 * - upsert updates existing record when found (idempotent)
 * - deliverableName is normalized to UPPER CASE and stripped
 * - sprint not found → 404
 * - getByProject / getBySprint / getByProjectAndDeliverable delegate correctly
 * - delete delegates correctly
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SprintDeliverableContributionService – Unit Tests")
class SprintDeliverableContributionServiceTest {

    @Mock private SprintDeliverableContributionRepository contributionRepository;
    @Mock private ProjectSprintRepository sprintRepository;

    @InjectMocks
    private SprintDeliverableContributionService service;

    private ProjectSprint sprint;

    @BeforeEach
    void setUp() {
        sprint = new ProjectSprint();
        sprint.setId(10L);
        sprint.setSprintNo(1);
        sprint.setTitle("Sprint 1");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // upsert() validation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("upsert() – validation")
    class UpsertValidationTests {

        @Test
        @DisplayName("contributionPct = 0 → 400 BAD_REQUEST")
        void pct_zero_rejected() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(10L, "PROPOSAL", 0, 1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("contributionPct = 101 → 400 BAD_REQUEST")
        void pct_over_100_rejected() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(10L, "PROPOSAL", 101, 1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("null contributionPct → 400 BAD_REQUEST")
        void null_pct_rejected() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(10L, "PROPOSAL", null, 1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("blank deliverableName → 400 BAD_REQUEST")
        void blank_deliverable_name_rejected() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(10L, "   ", 70, 1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("null deliverableName → 400 BAD_REQUEST")
        void null_deliverable_name_rejected() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(10L, null, 70, 1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("sprint not found → 404 NOT_FOUND")
        void sprint_not_found_throws_404() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(99L, "PROPOSAL", 70, 1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("contributionPct = 1 (boundary) → accepted")
        void pct_one_accepted() {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(contributionRepository.findBySprintIdAndDeliverableName(10L, "PROPOSAL"))
                    .thenReturn(Optional.empty());
            when(contributionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.upsert(10L, "PROPOSAL", 1, 1L));
        }

        @Test
        @DisplayName("contributionPct = 100 (boundary) → accepted")
        void pct_hundred_accepted() {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(contributionRepository.findBySprintIdAndDeliverableName(10L, "PROPOSAL"))
                    .thenReturn(Optional.empty());
            when(contributionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.upsert(10L, "PROPOSAL", 100, 1L));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // upsert() – create / update semantics
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("upsert() – create / update semantics")
    class UpsertSemanticsTests {

        @Test
        @DisplayName("New record created when not found")
        void creates_new_record_when_not_found() {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(contributionRepository.findBySprintIdAndDeliverableName(10L, "PROPOSAL"))
                    .thenReturn(Optional.empty());
            when(contributionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintDeliverableContribution result = service.upsert(10L, "PROPOSAL", 70, 99L);

            assertThat(result.getContributionPct()).isEqualTo(70);
            assertThat(result.getDeliverableName()).isEqualTo("PROPOSAL");
            assertThat(result.getSetByUserId()).isEqualTo(99L);
            verify(contributionRepository).save(any());
        }

        @Test
        @DisplayName("Existing record updated when found (idempotent upsert)")
        void updates_existing_record() {
            SprintDeliverableContribution existing = new SprintDeliverableContribution();
            existing.setSprint(sprint);
            existing.setDeliverableName("PROPOSAL");
            existing.setContributionPct(50);

            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(contributionRepository.findBySprintIdAndDeliverableName(10L, "PROPOSAL"))
                    .thenReturn(Optional.of(existing));
            when(contributionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintDeliverableContribution result = service.upsert(10L, "PROPOSAL", 80, 1L);

            assertThat(result.getContributionPct()).isEqualTo(80);
            verify(contributionRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("deliverableName is normalized to uppercase and stripped")
        void deliverable_name_normalized() {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(sprint));
            when(contributionRepository.findBySprintIdAndDeliverableName(10L, "PROPOSAL"))
                    .thenReturn(Optional.empty());
            when(contributionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintDeliverableContribution result = service.upsert(10L, " proposal ", 60, 1L);

            assertThat(result.getDeliverableName()).isEqualTo("PROPOSAL");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Query delegations
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Query methods – delegation")
    class QueryDelegationTests {

        @Test
        @DisplayName("getByProject delegates to repository")
        void get_by_project_delegates() {
            when(contributionRepository.findByProjectId(5L)).thenReturn(List.of());
            List<SprintDeliverableContribution> result = service.getByProject(5L);
            assertThat(result).isEmpty();
            verify(contributionRepository).findByProjectId(5L);
        }

        @Test
        @DisplayName("getBySprint delegates to repository")
        void get_by_sprint_delegates() {
            when(contributionRepository.findBySprintId(10L)).thenReturn(List.of());
            service.getBySprint(10L);
            verify(contributionRepository).findBySprintId(10L);
        }

        @Test
        @DisplayName("getByProjectAndDeliverable normalizes name and delegates")
        void get_by_project_and_deliverable_normalizes_and_delegates() {
            when(contributionRepository.findByProjectIdAndDeliverableName(5L, "SOW")).thenReturn(List.of());
            service.getByProjectAndDeliverable(5L, " sow ");
            verify(contributionRepository).findByProjectIdAndDeliverableName(5L, "SOW");
        }

        @Test
        @DisplayName("delete delegates with normalized name")
        void delete_normalizes_and_delegates() {
            service.delete(10L, " proposal ");
            verify(contributionRepository).deleteBySprintIdAndDeliverableName(10L, "PROPOSAL");
        }
    }
}
