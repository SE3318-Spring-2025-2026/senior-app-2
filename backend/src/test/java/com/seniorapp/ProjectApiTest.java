package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("null")
class ProjectApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProjectTemplateRepository templateRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearData() {
        jdbcTemplate.execute("DELETE FROM project_deliverable_rubrics");
        jdbcTemplate.execute("DELETE FROM project_evaluation_rubrics");
        jdbcTemplate.execute("DELETE FROM project_deliverables");
        jdbcTemplate.execute("DELETE FROM project_evaluations");
        jdbcTemplate.execute("DELETE FROM project_group_assignments");
        jdbcTemplate.execute("DELETE FROM project_sprints");
        projectRepository.deleteAllInBatch();
        templateRepository.deleteAllInBatch();
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void createProject_andDetail_andAssignGroup_happyPath() throws Exception {
        String templatePayload = """
                {
                  "name": "SP Template",
                  "description": "template",
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

        String templateResponse = mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templatePayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String templateId = objectMapper.readTree(templateResponse).path("data").path("templateId").asText();

        String createProjectPayload = """
                {
                  "templateId": %s,
                  "title": "Group 4 2026 Bitirme",
                  "term": "Spring 2026",
                  "groupId": 4
                }
                """.formatted(templateId);

        String projectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String projectId = objectMapper.readTree(projectResponse).path("data").path("projectId").asText();

        mockMvc.perform(get("/api/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Group 4 2026 Bitirme"))
                .andExpect(jsonPath("$.data.activeGroupId").value(4))
                .andExpect(jsonPath("$.data.sprints[0].deliverables[0].title").value("SOW"))
                .andExpect(jsonPath("$.data.sprints[0].evaluations[0].title").value("Teamwork"));
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void assignGroup_twice_returnsBadRequest() throws Exception {
        String templatePayload = """
                {
                  "name": "SP Template",
                  "description": "template",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": [
                    {
                      "sprintNo": 1,
                      "evaluations": [
                        {"title": "Eval", "weight": 100, "rubrics": [{"title": "R1", "criteriaType": "SOFT"}]}
                      ],
                      "deliverables": [
                        {"type": "DEMO", "title": "Demo", "weight": 100, "rubrics": [{"title": "R2", "criteriaType": "SOFT"}]}
                      ]
                    }
                  ]
                }
                """;
        String templateResponse = mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templatePayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String templateId = objectMapper.readTree(templateResponse).path("data").path("templateId").asText();

        String projectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId": %s, "title": "P", "term": "Spring 2026"}
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectResponse).path("data").path("projectId").asText();

        mockMvc.perform(post("/api/projects/{projectId}/group-assignment", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\": 11}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/{projectId}/group-assignment", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\": 12}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void createProject_withGroupId_filtersByGroupIdInList() throws Exception {
        String templatePayload = """
                {
                  "name": "SP Template",
                  "description": "template",
                  "term": "Spring 2026",
                  "projectStartDate": "2026-02-10",
                  "sprints": [
                    {
                      "sprintNo": 1,
                      "evaluations": [
                        {"title": "Eval", "weight": 100, "rubrics": [{"title": "R1", "criteriaType": "SOFT"}]}
                      ],
                      "deliverables": [
                        {"type": "DEMO", "title": "Demo", "weight": 100, "rubrics": [{"title": "R2", "criteriaType": "SOFT"}]}
                      ]
                    }
                  ]
                }
                """;
        String templateResponse = mockMvc.perform(post("/api/project-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templatePayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String templateId = objectMapper.readTree(templateResponse).path("data").path("templateId").asText();

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"templateId": %s, "title": "P1", "term": "Spring 2026", "groupId": 42}
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.projectId").isNumber());

        mockMvc.perform(get("/api/projects?groupId=42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("P1"))
                .andExpect(jsonPath("$.data[0].activeGroupId").value(42));
    }
}
