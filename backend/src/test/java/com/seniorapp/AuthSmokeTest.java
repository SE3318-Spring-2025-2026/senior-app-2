package com.seniorapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "github.client.id=dummy-test-id",
        "github.client.secret=dummy-test-secret",
        "app.jwt.secret=dummy-test-jwt-secret-key-must-be-long-enough"
})
@AutoConfigureMockMvc
public class AuthSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {

    }

    @Test
    void githubLoginEndpointShouldReturnAuthUrl() throws Exception {
        mockMvc.perform(get("/api/auth/github/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUrl").exists())
                .andExpect(jsonPath("$.authUrl").isString());
    }
}