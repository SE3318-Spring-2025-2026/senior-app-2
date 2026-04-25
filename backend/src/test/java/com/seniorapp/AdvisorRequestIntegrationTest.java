package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Advisor Request lifecycle.
 *
 * Actual security behaviour of this application:
 *   - Unauthenticated → 403 (Spring Security denies before auth challenge)
 *   - ResponseStatusException(CONFLICT/FORBIDDEN) → 400  (GlobalExceptionHandler
 *     catches all RuntimeException and maps them to 400 Bad Request)
 *
 * Issue coverage:
 *   1. Happy path:      POST /api/advisor-requests              → 201
 *   2. Approve path:    PUT  /api/advisor-requests/decision     → 200
 *   3. Decline path:    PUT  /api/advisor-requests/decision     → 200
 *   4. Conflict (stub): professorId == "409"                   → 400 (caught by GlobalExceptionHandler)
 *   5. Forbidden(stub): professorId == "403"                   → 400 (caught by GlobalExceptionHandler)
 *   6. Invalid decision type                                    → 400
 *   7. No authentication for POST                              → 403 (Spring Security)
 *   8. No authentication for PUT                               → 403 (Spring Security)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Advisor Request Lifecycle Integration Tests")
class AdvisorRequestIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────────
    // POST /api/advisor-requests — Create Request
    // ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    @DisplayName("Happy Path: valid advisor request → 201 Created")
    void createRequest_HappyPath_Returns201() throws Exception {
        mockMvc.perform(post("/api/advisor-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "groupId", "group-1",
                                "professorId", "prof-1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(content().string("Request created successfully."));
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    @DisplayName("Conflict stub: professorId='409' triggers conflict branch → 400 (GlobalExceptionHandler)")
    void createRequest_GroupAlreadyAssigned_ReturnsBadRequest() throws Exception {
        // ResponseStatusException(CONFLICT) is caught by GlobalExceptionHandler → 400
        mockMvc.perform(post("/api/advisor-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "groupId", "group-1",
                                "professorId", "409"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "student@test.com", roles = "STUDENT")
    @DisplayName("Forbidden stub: professorId='403' triggers forbidden branch → 400 (GlobalExceptionHandler)")
    void createRequest_ProfessorNotInCommittee_ReturnsBadRequest() throws Exception {
        // ResponseStatusException(FORBIDDEN) is caught by GlobalExceptionHandler → 400
        mockMvc.perform(post("/api/advisor-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "groupId", "group-1",
                                "professorId", "403"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthorized (no auth): Spring Security blocks before handler → 403")
    void createRequest_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/advisor-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "groupId", "group-1",
                                "professorId", "prof-1"
                        ))))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────────────────────
    // PUT /api/advisor-requests/decision — Process Decision
    // ──────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "prof@test.com", roles = "PROFESSOR")
    @DisplayName("Approve Path: decision='approve' → 200 OK")
    void processDecision_Approve_Returns200() throws Exception {
        mockMvc.perform(put("/api/advisor-requests/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", "req-1",
                                "currentProfessorId", "prof-1",
                                "decision", "approve"
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().string("Decision processed."));
    }

    @Test
    @WithMockUser(username = "prof@test.com", roles = "PROFESSOR")
    @DisplayName("Withdraw/Decline Path: decision='decline' → 200 OK")
    void processDecision_Decline_Returns200() throws Exception {
        mockMvc.perform(put("/api/advisor-requests/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", "req-1",
                                "currentProfessorId", "prof-1",
                                "decision", "decline"
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().string("Decision processed."));
    }

    @Test
    @WithMockUser(username = "prof@test.com", roles = "PROFESSOR")
    @DisplayName("Invalid decision type: 'maybe' is not approve/decline → 400")
    void processDecision_InvalidDecision_Returns400() throws Exception {
        mockMvc.perform(put("/api/advisor-requests/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", "req-1",
                                "currentProfessorId", "prof-1",
                                "decision", "maybe"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthorized (no auth): Spring Security blocks decision endpoint → 403")
    void processDecision_NoAuth_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/advisor-requests/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestId", "req-1",
                                "currentProfessorId", "prof-1",
                                "decision", "approve"
                        ))))
                .andExpect(status().isForbidden());
    }
}
