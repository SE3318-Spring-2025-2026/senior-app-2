package com.seniorapp;

import com.seniorapp.entity.Project;
import com.seniorapp.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProjectControllerSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();

        Project p1 = new Project();
        p1.setName("Senior App v2");
        p1.setTerm("Spring 2026");
        p1.setCommitteeId(10L);
        p1.setAdvisorId(5L);
        projectRepository.save(p1);
    }

    @Test
    @WithMockUser
    void getProjects_ShouldFilterByTerm() throws Exception {
        mockMvc.perform(get("/api/projects").param("term", "Spring 2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].term").value("Spring 2026"));
    }

    @Test
    @WithMockUser
    void getDeliverableStatuses_ShouldReturnStubbedData() throws Exception {
        mockMvc.perform(get("/api/projects/1/deliverables/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
