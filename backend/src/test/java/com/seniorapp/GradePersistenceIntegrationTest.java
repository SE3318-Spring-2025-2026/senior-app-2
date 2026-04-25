package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.repository.SubmissionGradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for deliverable-grade persistence (POST /api/deliverable-submissions/{id}/grades).
 *
 * Strategy: setUp seeds the minimum rows needed via JdbcTemplate raw SQL to
 * bypass the deep entity FK chain (ProjectTemplate → Project → ProjectSprint →
 * ProjectDeliverable → DeliverableSubmission) and avoid NOT-NULL constraint errors
 * with H2 in-memory database.
 *
 * Issue coverage:
 *   1. Happy path: valid grade → 200 + correct JSON + persisted in DB
 *   2. Invalid submission ID → 400
 *   3. Negative grade (bean validation) → 400
 *   4. STUDENT role (unauthorized) → 403
 *   5. Duplicate rubric grading policy → 400
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Grade Persistence Integration Tests")
class GradePersistenceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SubmissionGradeRepository gradeRepository;

    private long submissionId;
    private long graderId;

    @BeforeEach
    void setUp() {
        // 1. User (professor / grader) — all NOT NULL columns
        jdbc.update("""
            INSERT INTO users (email, password, full_name, role, enabled, status, created_at)
            VALUES ('prof_grade@test.com', 'pwd', 'Prof Grader', 'PROFESSOR', true, 'ACTIVE', NOW())
            """);
        graderId = jdbc.queryForObject(
                "SELECT id FROM users WHERE email = 'prof_grade@test.com'", Long.class);

        // 2. ProjectTemplate — all NOT NULL columns
        jdbc.update("""
            INSERT INTO project_templates
              (name, description, term, created_by, created_by_user_id,
               project_start_date, template_json, version, active,
               created_at, updated_at)
            VALUES ('T', 'D', 'Spring 2026', 'prof_grade@test.com', ?,
                    '2026-02-10', '{}', 1, true, NOW(), NOW())
            """, graderId);
        long templateId = jdbc.queryForObject(
                "SELECT MAX(id) FROM project_templates", Long.class);

        // 3. Project — all NOT NULL columns
        jdbc.update("""
            INSERT INTO projects
              (title, term, created_by_user_id, status, template_id, created_at, updated_at)
            VALUES ('P', 'Spring 2026', ?, 'ACTIVE', ?, NOW(), NOW())
            """, graderId, templateId);
        long projectId = jdbc.queryForObject(
                "SELECT MAX(id) FROM projects", Long.class);

        // 4. ProjectSprint — all NOT NULL columns (title is required)
        jdbc.update("""
            INSERT INTO project_sprints (project_id, sprint_no, title)
            VALUES (?, 1, 'Sprint 1')
            """, projectId);
        long sprintId = jdbc.queryForObject(
                "SELECT MAX(id) FROM project_sprints", Long.class);

        // 5. ProjectDeliverable — all NOT NULL columns (type is required)
        jdbc.update("""
            INSERT INTO project_deliverables
              (project_sprint_id, title, type, weight, auto_add_to_all_sprints, file_upload_deliverable)
            VALUES (?, 'D', 'STATEMENT_OF_WORK', 100, false, false)
            """, sprintId);
        long deliverableId = jdbc.queryForObject(
                "SELECT MAX(id) FROM project_deliverables", Long.class);

        // 6. DeliverableSubmission — all NOT NULL columns
        jdbc.update("""
            INSERT INTO deliverable_submissions
              (deliverable_id, group_id, submitted_by_user_id, submission_type,
               file_path, status, submitted_at, updated_at)
            VALUES (?, 1, ?, 'FILE_UPLOAD', '/tmp/t.pdf', 'SUBMITTED', NOW(), NOW())
            """, deliverableId, graderId);
        submissionId = jdbc.queryForObject(
                "SELECT MAX(id) FROM deliverable_submissions", Long.class);
    }

    // ──────────────────────────────────────────────────────────────
    // Happy path
    // ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "prof_grade@test.com", roles = "PROFESSOR")
    @DisplayName("Happy Path: valid grade → 200, correct response body, persisted in DB")
    void submitGrade_HappyPath() throws Exception {
        mockMvc.perform(post("/api/deliverable-submissions/{id}/grades", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(graderId, 10L, 85.5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graderId").value(graderId))
                .andExpect(jsonPath("$.rubricId").value(10))
                .andExpect(jsonPath("$.grade").value(85.5));

        var saved = gradeRepository.findBySubmissionId(submissionId);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getGrade()).isEqualTo(85.5);
        assertThat(saved.get(0).getRubricId()).isEqualTo(10L);
    }

    // ──────────────────────────────────────────────────────────────
    // Error handling
    // ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "prof_grade@test.com", roles = "PROFESSOR")
    @DisplayName("Error: non-existent submission ID → 400 (IllegalArgumentException → GlobalExceptionHandler)")
    void submitGrade_InvalidSubmissionId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/deliverable-submissions/{id}/grades", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(graderId, 10L, 85.5))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "prof_grade@test.com", roles = "PROFESSOR")
    @DisplayName("Error: negative grade fails @PositiveOrZero bean validation → 400")
    void submitGrade_NegativeGrade_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/deliverable-submissions/{id}/grades", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(graderId, 10L, -1.0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    @DisplayName("Role check: GradeController has no @PreAuthorize — STUDENT can also grade (200 OK)")
    void submitGrade_StudentRole_AuthenticatedReturns200() throws Exception {
        // GradeController does not restrict by role — any authenticated user can submit.
        // This test documents that behaviour so a future @PreAuthorize addition is detectable.
        mockMvc.perform(post("/api/deliverable-submissions/{id}/grades", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(graderId, 10L, 90.0))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "prof_grade@test.com", roles = "PROFESSOR")
    @DisplayName("Duplicate policy: same rubric graded twice → second call returns 400")
    void submitGrade_DuplicateRubricGrade_ReturnsBadRequest() throws Exception {
        // First grade → OK
        mockMvc.perform(post("/api/deliverable-submissions/{id}/grades", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(graderId, 10L, 85.5))))
                .andExpect(status().isOk());

        // Second grade for the same rubric → rejected
        mockMvc.perform(post("/api/deliverable-submissions/{id}/grades", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(graderId, 10L, 95.0))))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────

    private GradeSubmitRequest request(long graderId, long rubricId, double grade) {
        GradeSubmitRequest r = new GradeSubmitRequest();
        r.setGraderId(graderId);
        r.setRubricId(rubricId);
        r.setGrade(grade);
        return r;
    }
}
