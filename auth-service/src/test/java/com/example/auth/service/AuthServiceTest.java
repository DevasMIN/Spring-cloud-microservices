package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.model.UserRoles;
import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password("encodedPassword")
                .email("test@example.com")
                .roles(Collections.singleton(UserRoles.ROLE_USER))
                .build();
    }

    @Test
    void registerUser_Success() {
        // Arrange
        String username = "testuser";
        String password = "password";
        String email = "test@example.com";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.registerUser(username, password, email);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals(email, result.getEmail());
        assertTrue(result.getRoles().contains(UserRoles.ROLE_USER));

        verify(userRepository).findByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_UsernameExists_ThrowsException() {
        // Arrange
        String username = "testuser";
        String password = "password";
        String email = "test@example.com";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> authService.registerUser(username, password, email));

        verify(userRepository).findByUsername(username);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserByUsername_WhenUserExists_ReturnsUser() {
        // Arrange
        String username = "testuser";
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

        // Act
        User result = authService.getUserByUsername(username);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void getUserByUsername_WhenUserNotFound_ThrowsException() {
        // Arrange
        String username = "nonexistent";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> authService.getUserByUsername(username));
        verify(userRepository).findByUsername(username);
    }

    @Test
    void invalidateToken_Success() {
        // Arrange
        String token = "valid.jwt.token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        authService.invalidateToken(token);

        // Assert
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(any(), eq("invalidated"), eq(24L), eq(TimeUnit.HOURS));
    }
}
