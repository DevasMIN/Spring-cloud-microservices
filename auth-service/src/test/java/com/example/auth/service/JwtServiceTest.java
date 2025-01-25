package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.model.UserRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .roles(Collections.singleton(UserRoles.ROLE_USER))
                .build();

        String secretKey = "veryverysecretkeyforjwtthatisatleast32byteslong12345";
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 час
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // Act
        String token = jwtService.generateToken(testUser);

        // Assert
        assertNotNull(token);
        when(redisTemplate.hasKey(token)).thenReturn(false);
        assertTrue(jwtService.validateToken(token));
        assertEquals(testUser.getUsername(), jwtService.extractUsername(token));
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act & Assert
        assertFalse(jwtService.validateToken(invalidToken));
    }

    @Test
    void validateToken_WithInvalidatedToken_ShouldReturnFalse() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        when(redisTemplate.hasKey(token)).thenReturn(true);

        // Act
        boolean isValid = jwtService.validateToken(token);

        // Assert
        assertFalse(isValid);
        verify(redisTemplate).hasKey(token);
    }

    @Test
    void extractUsername_WithValidToken_ShouldReturnUsername() {
        // Arrange
        String token = jwtService.generateToken(testUser);

        // Act
        String username = jwtService.extractUsername(token);

        // Assert
        assertEquals(testUser.getUsername(), username);
    }

    @Test
    void invalidateToken_ShouldStoreInRedis() {
        // Arrange
        String token = jwtService.generateToken(testUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        jwtService.invalidateToken(token);

        // Assert
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(token, "invalidated", 24L, TimeUnit.HOURS);
    }
}
