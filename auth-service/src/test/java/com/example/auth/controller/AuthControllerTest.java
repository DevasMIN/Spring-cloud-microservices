package com.example.auth.controller;

import com.example.auth.dto.AuthRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.User;
import com.example.auth.model.UserRoles;
import com.example.auth.service.AuthService;
import com.example.auth.service.CustomUserDetailsService;
import com.example.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.3")
            .withDatabaseName("auth_db_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @SuppressWarnings("resource")
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0.11")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("spring.redis.password", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private User testUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .roles(Collections.singleton(UserRoles.ROLE_USER))
                .build();

        testToken = "test.jwt.token";

        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(testUser);
    }

    @Test
    void register_Success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");

        when(authService.registerUser("testuser", "password", "test@example.com"))
                .thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn(testToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(testToken))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void login_Success() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        Authentication authentication = new UsernamePasswordAuthenticationToken(testUser, null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(testUser)).thenReturn(testToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(testToken))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void validateToken_ValidToken() throws Exception {
        // Arrange
        when(jwtService.extractUsername(testToken)).thenReturn("testuser");
        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
        when(jwtService.validateToken(testToken)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void validateToken_InvalidToken() throws Exception {
        // Arrange
        when(jwtService.extractUsername(testToken)).thenReturn("testuser");
        when(authService.getUserByUsername("testuser")).thenReturn(testUser);
        when(jwtService.validateToken(testToken)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/validate")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void logout_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk());
    }
}
