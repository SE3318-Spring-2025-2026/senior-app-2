package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GradeControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeliverableSubmissionRepository submissionRepository;

    @Autowired
    private SubmissionGradeRepository gradeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testGrader;
    private DeliverableSubmission testSubmission;

    @BeforeEach
    void setUp() {
        gradeRepository.deleteAll();
        submissionRepository.deleteAll();
        userRepository.deleteAll();

        testGrader = new User();
        testGrader.setEmail("grader@seniorapp.com");
        testGrader.setFullName("Test Grader");
        testGrader.setPassword("password");
        testGrader.setRole(Role.PROFESSOR);
        testGrader = userRepository.save(testGrader);

        testSubmission = new DeliverableSubmission();
        testSubmission = submissionRepository.save(testSubmission);
    }

    @Test
    @WithMockUser
    void submitGrade_ShouldSaveGradeAndReturnResponse() throws Exception {
        GradeSubmitRequest request = new GradeSubmitRequest();
        request.setGraderId(testGrader.getId());
        request.setRubricId(1L);
        request.setGrade(85.5);

        mockMvc.perform(post("/api/deliverable-submissions/" + testSubmission.getId() + "/grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grade").value(85.5))
                .andExpect(jsonPath("$.rubricId").value(1))
                .andExpect(jsonPath("$.submissionId").value(testSubmission.getId()))
                .andExpect(jsonPath("$.graderId").value(testGrader.getId()));
    }

    @Test
    @WithMockUser
    void getGrades_ShouldReturnGradesList() throws Exception {
        SubmissionGrade grade = new SubmissionGrade();
        grade.setSubmissionId(testSubmission.getId());
        grade.setGraderId(testGrader.getId());
        grade.setRubricId(2L);
        grade.setGrade(90.0);
        gradeRepository.save(grade);

        mockMvc.perform(get("/api/deliverable-submissions/" + testSubmission.getId() + "/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].grade").value(90.0));
    }
}
