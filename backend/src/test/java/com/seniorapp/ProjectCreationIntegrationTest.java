package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.project.ProjectDtos.CreateProjectRequest;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectTemplateRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Project Creation Integration Tests")
class ProjectCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectTemplateRepository templateRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectGroupAssignmentRepository assignmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String validTemplateId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean DB
        jdbcTemplate.execute("DELETE FROM project_committee_professors");
        jdbcTemplate.execute("DELETE FROM project_committees");
        jdbcTemplate.execute("DELETE FROM project_deliverable_rubrics");
        jdbcTemplate.execute("DELETE FROM project_evaluation_rubrics");
        jdbcTemplate.execute("DELETE FROM project_deliverables");
        jdbcTemplate.execute("DELETE FROM project_evaluations");
        jdbcTemplate.execute("DELETE FROM project_group_assignments");
        jdbcTemplate.execute("DELETE FROM project_sprints");
        projectRepository.deleteAllInBatch();
        templateRepository.deleteAllInBatch();

        // Prepare a valid template for testing
        String templatePayload = """
                {
                  "name": "Integration Test Template",
                  "description": "Template for testing project creation",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": [
                    {
                      "sprintNo": 1,
                      "title": "Sprint 1",
                      "startDate": "2026-02-10",
                      "endDate": "2026-02-20",
                      "evaluations": [
                        {
                          "title": "Teamwork",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Participation", "criteriaType": "SOFT"}
                          ]
                        }
                      ],
                      "deliverables": [
                        {
                          "type": "STATEMENT_OF_WORK",
                          "title": "SOW",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Quality", "criteriaType": "SOFT"}
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        // To create a template, user needs to be COORDINATOR
        // We do this by calling the API directly or saving via repository.
        // Let's call the API to ensure the template JSON is correctly formed in DB.
    }

    private String createTemplateAndGetId() throws Exception {
        String templatePayload = """
                {
                  "name": "Integration Test Template",
                  "description": "Template for testing project creation",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": [
                    {
                      "sprintNo": 1,
                      "title": "Sprint 1",
                      "startDate": "2026-02-10",
                      "endDate": "2026-02-20",
                      "evaluations": [
                        {
                          "title": "Teamwork",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Participation", "criteriaType": "SOFT"}
                          ]
                        }
                      ],
                      "deliverables": [
                        {
                          "type": "STATEMENT_OF_WORK",
                          "title": "SOW",
                          "weight": 100,
                          "rubrics": [
                            {"title": "Quality", "criteriaType": "SOFT"}
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;
        String response = mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templatePayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("templateId").asText();
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    @DisplayName("Happy Path: Create project successfully from a valid template")
    void createProject_HappyPath() throws Exception {
        String templateId = createTemplateAndGetId();

        CreateProjectRequest request = new CreateProjectRequest();
        request.setTemplateId(Long.parseLong(templateId));
        request.setTitle("Senior App Implementation");
        request.setTerm("Spring 2026");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectId").isNumber());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    @DisplayName("Error Handling: Fail to create project with invalid template ID")
    void createProject_InvalidTemplateId_ReturnsNotFound() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTemplateId(99999L); // Non-existent
        request.setTitle("Senior App Implementation");
        request.setTerm("Spring 2026");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    @DisplayName("Error Handling: Fail to create project with missing title and term")
    void createProject_MissingFields_ReturnsBadRequest() throws Exception {
        String templateId = createTemplateAndGetId();

        // Missing Title and Term
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTemplateId(Long.parseLong(templateId));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "STUDENT")
    @DisplayName("Error Handling: Unauthorized access for STUDENT role")
    void createProject_UnauthorizedRole_ReturnsForbidden() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTemplateId(1L);
        request.setTitle("Student Attempt");
        request.setTerm("Spring 2026");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    @DisplayName("Duplicate Policy: Cannot assign group twice to the same project")
    void createProject_DuplicateGroupAssignment_ReturnsBadRequest() throws Exception {
        String templateId = createTemplateAndGetId();

        // 1. Create Project
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTemplateId(Long.parseLong(templateId));
        request.setTitle("Group Test Project");
        request.setTerm("Spring 2026");

        String projectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(projectResponse).path("data").path("projectId").asText();

        // 2. Assign Group 10
        mockMvc.perform(post("/api/projects/{projectId}/group-assignment", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\": 10}"))
                .andExpect(status().isOk());

        // 3. Try to Assign Group 11 to the same project -> Should Fail
        mockMvc.perform(post("/api/projects/{projectId}/group-assignment", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\": 11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
