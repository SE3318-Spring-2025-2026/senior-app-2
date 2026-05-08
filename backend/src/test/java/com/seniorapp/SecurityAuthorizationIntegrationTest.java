package com.seniorapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ----------------------------------------------------
    // GET /api/projects
    // @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    // ----------------------------------------------------
    
    @Test
    @WithMockUser(roles = "STUDENT")
    void getProjects_WhenRoleStudent_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/api/projects"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void getProjects_WhenRoleCoordinator_ShouldNotBeForbidden() throws Exception {
        // Empty DB will return 200 OK, not 403
        mockMvc.perform(get("/api/projects"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void getProjects_WhenRoleProfessor_ShouldNotBeForbidden() throws Exception {
        mockMvc.perform(get("/api/projects"))
               .andExpect(status().isOk());
    }

    // ----------------------------------------------------
    // POST /api/projects/1/committees
    // @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    // ----------------------------------------------------
    
    @Test
    @WithMockUser(roles = "PROFESSOR")
    void createCommittee_WhenRoleProfessor_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/projects/1/committees")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createCommittee_WhenRoleStudent_ShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/projects/1/committees")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{}"))
               .andExpect(status().isForbidden());
    }
}
