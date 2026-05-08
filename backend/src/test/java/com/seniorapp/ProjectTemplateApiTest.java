package com.seniorapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.repository.ProjectTemplateRepository;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class ProjectTemplateApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProjectTemplateRepository projectTemplateRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetTemplates() {
        jdbcTemplate.execute("DELETE FROM template_committee_professors");
        jdbcTemplate.execute("DELETE FROM template_committees");
        jdbcTemplate.execute("DELETE FROM project_committee_professors");
        jdbcTemplate.execute("DELETE FROM project_committees");
        jdbcTemplate.execute("DELETE FROM project_deliverable_rubrics");
        jdbcTemplate.execute("DELETE FROM project_evaluation_rubrics");
        jdbcTemplate.execute("DELETE FROM project_deliverables");
        jdbcTemplate.execute("DELETE FROM project_evaluations");
        jdbcTemplate.execute("DELETE FROM project_group_assignments");
        jdbcTemplate.execute("DELETE FROM project_sprints");
        jdbcTemplate.execute("DELETE FROM projects");
        projectTemplateRepository.deleteAllInBatch();
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void createTemplate_returnsSuccess() throws Exception {
        String payload = """
                {
                  "name": "Senior Project 2026 Template",
                  "description": "Default template for senior project workflow",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": [
                    {
                      "sprintNo": 1,
                      "title": "Sprint 1",
                      "evaluations": [
                        {
                          "type": "ADVISOR_EVALUATION",
                          "title": "Sprint 1 Team Performance",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Scrum Participation", "criteriaType": "SOFT", "maxScore": 100}
                          ]
                        }
                      ],
                      "deliverables": [
                        {
                          "type": "PROPOSAL",
                          "title": "Proposal Submission",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Problem Definition", "criteriaType": "SOFT", "maxScore": 100}
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.templateId").isNumber());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void createTemplate_withInvalidSprints_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "name": "Bad Template",
                  "description": "invalid",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": []
                }
                """;

        mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void listTemplates_returnsOk() throws Exception {
        mockMvc.perform(get("/api/project-templates").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void detailEndpoint_returnsSavedPayload() throws Exception {
        String payload = """
                {
                  "name": "Template Detail Check",
                  "description": "detail response test",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": [
                    {
                      "sprintNo": 1,
                      "evaluations": [
                        {
                          "type": "ADVISOR_EVALUATION",
                          "title": "Sprint 1 Team Performance",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Scrum Participation", "criteriaType": "SOFT", "maxScore": 100}
                          ]
                        }
                      ],
                      "deliverables": [
                        {
                          "type": "PROPOSAL",
                          "title": "P1",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Problem Definition", "criteriaType": "SOFT", "maxScore": 100}
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        String response = mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String templateId = objectMapper.readTree(response).path("data").path("templateId").asText();

        mockMvc.perform(get("/api/project-templates/{templateId}", templateId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.payload.name").value("Template Detail Check"))
                .andExpect(jsonPath("$.data.payload.sprints[0].sprintNo").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void listProfessors_includesCoordinatorsAndProfessorsOnly() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String professorEmail = "committee-prof-" + suffix + "@test.com";
        String coordinatorEmail = "committee-coord-" + suffix + "@test.com";
        String studentEmail = "committee-student-" + suffix + "@test.com";

        insertUser(professorEmail, "Committee Professor", "PROFESSOR");
        insertUser(coordinatorEmail, "Committee Coordinator", "COORDINATOR");
        insertUser(studentEmail, "Committee Student", "STUDENT");

        mockMvc.perform(get("/api/project-templates/professors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[?(@.email=='" + professorEmail + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.email=='" + coordinatorEmail + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.email=='" + studentEmail + "')]").isEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void addProfessorToCommittee_acceptsCoordinatorAndPreventsDuplicateAssignment() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Long coordinatorId = insertUser("assign-coord-" + suffix + "@test.com", "Assignable Coordinator", "COORDINATOR");
        Long templateId = createTemplateForCommitteeTests();

        String committeeResponse = mockMvc.perform(post("/api/project-templates/{templateId}/committees", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long committeeId = objectMapper.readTree(committeeResponse).path("data").path("committeeId").asLong();

        String request = """
                {
                  "professorUserId": %d
                }
                """.formatted(coordinatorId);

        mockMvc.perform(post("/api/project-templates/{templateId}/committees/{committeeId}/professors", templateId, committeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.professors[0].userId").value(coordinatorId))
                .andExpect(jsonPath("$.data.professors[0].email").value("assign-coord-" + suffix + "@test.com"));

        mockMvc.perform(post("/api/project-templates/{templateId}/committees/{committeeId}/professors", templateId, committeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.professors.length()").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void addProfessorToCommittee_rejectsInvalidRole() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Long studentId = insertUser("assign-student-" + suffix + "@test.com", "Not Assignable Student", "STUDENT");
        Long templateId = createTemplateForCommitteeTests();

        String committeeResponse = mockMvc.perform(post("/api/project-templates/{templateId}/committees", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long committeeId = objectMapper.readTree(committeeResponse).path("data").path("committeeId").asLong();

        String request = """
                {
                  "professorUserId": %d
                }
                """.formatted(studentId);

        mockMvc.perform(post("/api/project-templates/{templateId}/committees/{committeeId}/professors", templateId, committeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Selected user must be a professor or coordinator."));
    }

    private Long insertUser(String email, String fullName, String role) {
        jdbcTemplate.update("""
                INSERT INTO users (email, password, full_name, role, enabled, status, created_at)
                VALUES (?, 'pwd', ?, ?, true, 'ACTIVE', NOW())
                """, email, fullName, role);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private Long createTemplateForCommitteeTests() throws Exception {
        String payload = """
                {
                  "name": "Committee Template",
                  "description": "template for committee assignment tests",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": [
                    {
                      "sprintNo": 1,
                      "evaluations": [
                        {
                          "title": "Sprint Evaluation",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Evaluation Rubric", "criteriaType": "SOFT", "maxScore": 100}
                          ]
                        }
                      ],
                      "deliverables": [
                        {
                          "title": "Deliverable",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Deliverable Rubric", "criteriaType": "SOFT", "maxScore": 100}
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
        String response = mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").path("templateId").asLong();
    }
}
