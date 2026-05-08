package com.seniorapp.service;

import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.SprintAdvisorGrade;
import com.seniorapp.entity.SprintGradeType;
import com.seniorapp.repository.ProjectSprintRepository;
import com.seniorapp.repository.SprintAdvisorGradeRepository;
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
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Aggressive unit tests for {@link SprintAdvisorGradeService}.
 *
 * Edge cases:
 * - All valid grades (A/B/C/D/F) map to correct numeric values
 * - Invalid letter (E, Z, empty, null) -> 400
 * - Sprint not found -> 404
 * - Upsert creates new / updates existing
 * - computeScalar: AVG of multiple grades / 100
 * - computeScalar with empty list -> 400
 * - toNumeric all 5 valid letters
 * - lowercase input is normalized
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SprintAdvisorGradeService - Unit Tests")
class SprintAdvisorGradeServiceTest {

    @Mock private SprintAdvisorGradeRepository gradeRepository;
    @Mock private ProjectSprintRepository sprintRepository;

    @InjectMocks
    private SprintAdvisorGradeService service;

    private ProjectSprint sprint;

    @BeforeEach
    void setUp() {
        sprint = new ProjectSprint();
        sprint.setId(1L);
        sprint.setSprintNo(1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Soft-grade numeric mapping
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("toNumeric() – grade letter mapping")
    class ToNumericTests {

        @Test @DisplayName("A -> 100.0")
        void a_maps_to_100() { assertThat(service.toNumeric("A")).isEqualTo(100.0); }

        @Test @DisplayName("B -> 80.0")
        void b_maps_to_80() { assertThat(service.toNumeric("B")).isEqualTo(80.0); }

        @Test @DisplayName("C -> 60.0")
        void c_maps_to_60() { assertThat(service.toNumeric("C")).isEqualTo(60.0); }

        @Test @DisplayName("D -> 50.0")
        void d_maps_to_50() { assertThat(service.toNumeric("D")).isEqualTo(50.0); }

        @Test @DisplayName("F -> 0.0")
        void f_maps_to_0() { assertThat(service.toNumeric("F")).isEqualTo(0.0); }

        @Test @DisplayName("lowercase 'b' is normalized and accepted")
        void lowercase_normalized() { assertThat(service.toNumeric("b")).isEqualTo(80.0); }

        @Test @DisplayName("Invalid letter 'E' -> 400")
        void invalid_letter_throws() {
            assertThrows(ResponseStatusException.class, () -> service.toNumeric("E"));
        }

        @Test @DisplayName("null -> 400")
        void null_grade_throws() {
            assertThrows(ResponseStatusException.class, () -> service.toNumeric(null));
        }

        @Test @DisplayName("blank -> 400")
        void blank_grade_throws() {
            assertThrows(ResponseStatusException.class, () -> service.toNumeric("   "));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // computeScalar()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("computeScalar() – deliverable scalar formula")
    class ComputeScalarTests {

        @Test
        @DisplayName("AVG(B=80, A=100) / 100 = 0.9")
        void two_grade_scalar_matches_spec_example() {
            // Spec example: Scrum Sprint1+2 → AVG(B,A) = 90 → scalar 0.9
            double scalar = service.computeScalar(List.of("B", "A"));
            assertThat(scalar).isCloseTo(0.9, offset(0.001));
        }

        @Test
        @DisplayName("AVG(B=80, A=100, A=100) / 100 = 0.9333")
        void three_grade_scalar() {
            double scalar = service.computeScalar(List.of("B", "A", "A"));
            assertThat(scalar).isCloseTo(0.9333, offset(0.001));
        }

        @Test
        @DisplayName("AVG(C=60, B=80) / 100 = 0.7 (spec Work/Code Review example)")
        void code_review_two_sprints_scalar() {
            double scalar = service.computeScalar(List.of("C", "B"));
            assertThat(scalar).isCloseTo(0.7, offset(0.001));
        }

        @Test
        @DisplayName("Single F -> scalar = 0.0")
        void single_f_scalar_zero() {
            assertThat(service.computeScalar(List.of("F"))).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Empty list -> 400 BAD_REQUEST")
        void empty_list_throws() {
            assertThrows(ResponseStatusException.class, () -> service.computeScalar(List.of()));
        }

        @Test
        @DisplayName("null list -> 400 BAD_REQUEST")
        void null_list_throws() {
            assertThrows(ResponseStatusException.class, () -> service.computeScalar(null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // upsert()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("upsert() – create / update semantics")
    class UpsertTests {

        @Test
        @DisplayName("Creates new record when none exists")
        void creates_new_record() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(gradeRepository.findBySprintIdAndGroupIdAndGradeType(1L, 10L, SprintGradeType.SCRUM))
                    .thenReturn(Optional.empty());
            when(gradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintAdvisorGrade result = service.upsert(1L, 10L, 99L, SprintGradeType.SCRUM, "B", "Good sprint");

            assertThat(result.getSoftGrade()).isEqualTo("B");
            assertThat(result.getGradeType()).isEqualTo(SprintGradeType.SCRUM);
            assertThat(result.getAdvisorUserId()).isEqualTo(99L);
            verify(gradeRepository).save(any());
        }

        @Test
        @DisplayName("Updates existing record (idempotent upsert)")
        void updates_existing_record() {
            SprintAdvisorGrade existing = new SprintAdvisorGrade();
            existing.setSprint(sprint);
            existing.setGroupId(10L);
            existing.setGradeType(SprintGradeType.SCRUM);
            existing.setSoftGrade("A");

            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(gradeRepository.findBySprintIdAndGroupIdAndGradeType(1L, 10L, SprintGradeType.SCRUM))
                    .thenReturn(Optional.of(existing));
            when(gradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintAdvisorGrade result = service.upsert(1L, 10L, 99L, SprintGradeType.SCRUM, "C", null);

            assertThat(result.getSoftGrade()).isEqualTo("C");
            verify(gradeRepository, times(1)).save(existing);
        }

        @Test
        @DisplayName("Sprint not found -> 404 NOT_FOUND")
        void sprint_not_found_throws_404() {
            when(sprintRepository.findById(99L)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsert(99L, 10L, 1L, SprintGradeType.CODE_REVIEW, "A", null));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("Invalid soft-grade -> 400 before hitting repository")
        void invalid_grade_throws_before_db() {
            assertThrows(ResponseStatusException.class,
                    () -> service.upsert(1L, 10L, 1L, SprintGradeType.SCRUM, "X", null));
            verifyNoInteractions(sprintRepository, gradeRepository);
        }

        @Test
        @DisplayName("Lowercase grade letter is normalized to uppercase")
        void lowercase_grade_normalized() {
            when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));
            when(gradeRepository.findBySprintIdAndGroupIdAndGradeType(1L, 10L, SprintGradeType.CODE_REVIEW))
                    .thenReturn(Optional.empty());
            when(gradeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SprintAdvisorGrade result = service.upsert(1L, 10L, 1L, SprintGradeType.CODE_REVIEW, "a", null);
            assertThat(result.getSoftGrade()).isEqualTo("A");
        }
    }
}
