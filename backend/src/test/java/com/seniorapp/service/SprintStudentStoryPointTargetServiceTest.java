package com.seniorapp.service;

import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.SprintStudentStoryPointTarget;
import com.seniorapp.repository.ProjectSprintRepository;
import com.seniorapp.repository.SprintStudentStoryPointTargetRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Aggressive unit tests for {@link SprintStudentStoryPointTargetService}.
 *
 * Edge cases:
 * - targetPoints = null -> 400
 * - targetPoints = -1 -> 400
 * - targetPoints = 0 -> valid (coordinator can zero-out a student)
 * - sprint not found -> 404
 * - target not found for student+sprint -> 404
 * - upsert creates new record
 * - upsert updates existing record
 * - getBySprintId delegates to repository
 * - getByProjectAndStudent delegates to repository
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SprintStudentStoryPointTargetService - Unit Tests")
class SprintStudentStoryPointTargetServiceTest {

    @Mock private SprintStudentStoryPointTargetRepository targetRepository;
    @Mock private ProjectSprintRepository sprintRepository;

    @InjectMocks
    private SprintStudentStoryPointTargetService service;

    private ProjectSprint sprint;

    @BeforeEach
    void setUp() {
        sprint = new ProjectSprint();
        sprint.setId(1L);
        sprint.setSprintNo(1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // upsert() – validation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("upsert() – validation")
    class UpsertValidationTests {

        @Test
        @DisplayName("null targetPoints -> 400 BAD_REQUEST")
        void null_target_rejected() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(1L, 5L, null, 99L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verifyNoInteractions(sprintRepository, targetRepository);
        }

        @Test
        @DisplayName("negative targetPoints -> 400 BAD_REQUEST")
        void negative_target_rejected() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(1L, 5L, -1, 99L));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verifyNoInteractions(sprintRepository, targetRepository);
        }

        @Test
        @DisplayName("targetPoints = 0 -> valid (excludes student from individual scoring)")
        void zero_target_is_valid() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(targetRepository.findBySprintIdAndStudentUserId(1L, 5L)).thenReturn(Optional.empty());
            when(targetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.upsert(1L, 5L, 0, 99L));
        }

        @Test
        @DisplayName("sprint not found -> 404 NOT_FOUND")
        void sprint_not_found_throws_404() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(99L, 5L, 5, 1L));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // upsert() – create / update semantics
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("upsert() – create / update semantics")
    class UpsertSemanticsTests {

        @Test
        @DisplayName("Creates new record when none exists")
        void creates_new_record() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(targetRepository.findBySprintIdAndStudentUserId(1L, 5L)).thenReturn(Optional.empty());
            when(targetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintStudentStoryPointTarget result = service.upsert(1L, 5L, 5, 99L);

            assertThat(result.getTargetPoints()).isEqualTo(5);
            assertThat(result.getStudentUserId()).isEqualTo(5L);
            assertThat(result.getSetByUserId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("Updates existing record (idempotent upsert)")
        void updates_existing_record() {
            SprintStudentStoryPointTarget existing = new SprintStudentStoryPointTarget();
            existing.setSprint(sprint);
            existing.setStudentUserId(5L);
            existing.setTargetPoints(3);

            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(targetRepository.findBySprintIdAndStudentUserId(1L, 5L)).thenReturn(Optional.of(existing));
            when(targetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintStudentStoryPointTarget result = service.upsert(1L, 5L, 8, 99L);

            assertThat(result.getTargetPoints()).isEqualTo(8);
            verify(targetRepository, times(1)).save(existing);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Query methods
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Query methods")
    class QueryTests {

        @Test
        @DisplayName("getTarget - found -> returns entity")
        void get_target_found() {
            SprintStudentStoryPointTarget target = new SprintStudentStoryPointTarget();
            target.setTargetPoints(5);
            when(targetRepository.findBySprintIdAndStudentUserId(1L, 5L)).thenReturn(Optional.of(target));

            SprintStudentStoryPointTarget result = service.getTarget(1L, 5L);
            assertThat(result.getTargetPoints()).isEqualTo(5);
        }

        @Test
        @DisplayName("getTarget - not found -> 404")
        void get_target_not_found() {
            when(targetRepository.findBySprintIdAndStudentUserId(1L, 5L)).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class, () -> service.getTarget(1L, 5L));
        }

        @Test
        @DisplayName("getBySprintId delegates to repository")
        void get_by_sprint_delegates() {
            when(targetRepository.findBySprintId(1L)).thenReturn(List.of());
            service.getBySprintId(1L);
            verify(targetRepository).findBySprintId(1L);
        }

        @Test
        @DisplayName("getByProjectAndStudent delegates to repository")
        void get_by_project_and_student_delegates() {
            when(targetRepository.findByProjectIdAndStudentUserId(10L, 5L)).thenReturn(List.of());
            service.getByProjectAndStudent(10L, 5L);
            verify(targetRepository).findByProjectIdAndStudentUserId(10L, 5L);
        }
    }
}
