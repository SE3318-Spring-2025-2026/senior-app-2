package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.controller.AuthController;
import com.seniorapp.dto.AuthResponse;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.service.AuthService;
import com.seniorapp.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Güvenlik filtrelerini devre dışı bırakıyoruz ki @WithMockUser kolayca çalışsın
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private LogService logService;

    @MockBean
    private com.seniorapp.security.JwtUtil jwtUtil;

    @MockBean
    private com.seniorapp.repository.UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // LogService mock'unu sessize al (void olmadığı için thenReturn kullanıyoruz)
        Mockito.when(logService.saveAuthLog(any(), any(), anyString(), anyString(), anyString(), any())).thenReturn(null);
        Mockito.when(logService.saveLog(any(), any(), anyString(), anyString(), anyString(), anyString(), anyString(), any())).thenReturn(null);
    }

    @Test
    void testLoginEndpoint_Success() throws Exception {
        // 1. Hazırlık (Arrange)
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", "admin@senior.app");
        loginRequest.put("password", "12345");

        User dummyUser = new User();
        dummyUser.setId(1L);
        dummyUser.setEmail("admin@senior.app");
        dummyUser.setRole(Role.ADMIN);
        
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(1L, "admin@senior.app", "Test User", "ADMIN", null);
        AuthResponse mockResponse = new AuthResponse("mock-jwt-token", userInfo);

        when(authService.staffLogin(eq("admin@senior.app"), eq("12345"))).thenReturn(mockResponse);

        // 2. Aksiyon ve Doğrulama (Act & Assert)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.user.email").value("admin@senior.app"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRegisterStaffEndpoint_Success() throws Exception {
        // 1. Hazırlık (Arrange)
        Map<String, String> registerRequest = new HashMap<>();
        registerRequest.put("email", "newstaff@senior.app");
        registerRequest.put("fullName", "New Staff");
        registerRequest.put("role", "PROFESSOR");

        User newUser = new User();
        newUser.setId(2L);
        newUser.setEmail("newstaff@senior.app");

        when(authService.registerStaff(eq("newstaff@senior.app"), eq("New Staff"), eq(Role.PROFESSOR)))
                .thenReturn(newUser);
        when(authService.generatePasswordResetToken(eq(2L)))
                .thenReturn("mock-reset-token");

        // 2. Aksiyon ve Doğrulama (Act & Assert)
        mockMvc.perform(post("/api/auth/register-staff")
                        .with(csrf()) // Güvenlik tokenı
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newstaff@senior.app"))
                .andExpect(jsonPath("$.resetLink").value("/reset-password?token=mock-reset-token"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testChangeUserRoleEndpoint_Success() throws Exception {
        // 1. Hazırlık (Arrange)
        Map<String, Object> changeRoleRequest = new HashMap<>();
        changeRoleRequest.put("userId", 3L);
        changeRoleRequest.put("role", "COORDINATOR");

        AuthResponse.UserInfo updatedUserInfo = new AuthResponse.UserInfo(3L, "user@senior.app", "Test User", "COORDINATOR", null);

        when(authService.changeUserRole(eq(3L), eq(Role.COORDINATOR))).thenReturn(updatedUserInfo);

        // 2. Aksiyon ve Doğrulama (Act & Assert)
        mockMvc.perform(put("/api/auth/users/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRoleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("COORDINATOR"));
    }
}
