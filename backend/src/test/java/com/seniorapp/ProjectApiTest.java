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

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void listProfessors_includesCoordinatorsAndProfessorsOnly() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String professorEmail = "project-committee-prof-" + suffix + "@test.com";
        String coordinatorEmail = "project-committee-coord-" + suffix + "@test.com";
        String studentEmail = "project-committee-student-" + suffix + "@test.com";

        insertUser(professorEmail, "Project Committee Professor", "PROFESSOR");
        insertUser(coordinatorEmail, "Project Committee Coordinator", "COORDINATOR");
        insertUser(studentEmail, "Project Committee Student", "STUDENT");

        mockMvc.perform(get("/api/projects/professors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data[?(@.email=='" + professorEmail + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.email=='" + coordinatorEmail + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data[?(@.email=='" + studentEmail + "')]").isEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "COORDINATOR")
    void addProfessorToCommittee_acceptsCoordinatorAndRejectsInvalidRole() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Long coordinatorId = insertUser("project-assign-coord-" + suffix + "@test.com", "Assignable Coordinator", "COORDINATOR");
        Long studentId = insertUser("project-assign-student-" + suffix + "@test.com", "Not Assignable Student", "STUDENT");
        Long projectId = createProjectForCommitteeTests();

        String committeeResponse = mockMvc.perform(post("/api/projects/{projectId}/committees", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long committeeId = objectMapper.readTree(committeeResponse).path("data").path("committeeId").asLong();

        String assignCoordinatorRequest = """
                {
                  "professorUserId": %d
                }
                """.formatted(coordinatorId);

        mockMvc.perform(post("/api/projects/{projectId}/committees/{committeeId}/professors", projectId, committeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignCoordinatorRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.professors[0].userId").value(coordinatorId));

        String assignStudentRequest = """
                {
                  "professorUserId": %d
                }
                """.formatted(studentId);

        mockMvc.perform(post("/api/projects/{projectId}/committees/{committeeId}/professors", projectId, committeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignStudentRequest))
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

    private Long createProjectForCommitteeTests() throws Exception {
        String templatePayload = """
                {
                  "name": "Project Committee Template",
                  "description": "template for project committee tests",
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
                                {"templateId": %s, "title": "Project Committee Test", "term": "Spring 2026"}
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(projectResponse).path("data").path("projectId").asLong();
    }
}
