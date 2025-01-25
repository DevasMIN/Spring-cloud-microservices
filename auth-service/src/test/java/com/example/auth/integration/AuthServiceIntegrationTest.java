package com.example.auth.integration;

import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.AuthService;
import com.example.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class AuthServiceIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    public static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15.3")
            .withDatabaseName("authdb")
            .withUsername("user")
            .withPassword("password");

    @SuppressWarnings("resource")
    @Container
    public static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0.11")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Настройка JDBC
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

        // Настройка Redis
        registry.add("spring.redis.host", redisContainer::getHost);
        registry.add("spring.redis.port", () -> redisContainer.getMappedPort(6379));

        // Настройка JWT
        registry.add("jwt.secret", () -> "MyIntegrationTestSecretKeyWhichIsLongEnoughForHS256");
        registry.add("jwt.expiration", () -> 3600000L); // 1 час
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushAll();
    }

    @Test
    void registerAndAuthenticateUser() {
        String username = "integrationUser";
        String password = "securePassword";
        String email = "integration@example.com";

        // Регистрация пользователя
        User registeredUser = authService.registerUser(username, password, email);
        assertNotNull(registeredUser);
        assertEquals(username, registeredUser.getUsername());
        assertTrue(passwordEncoder.matches(password, registeredUser.getPassword()));

        // Аутентификация пользователя через AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        assertTrue(authentication.isAuthenticated());
        assertEquals(username, ((User) authentication.getPrincipal()).getUsername());
    }

    @Test
    void generateAndValidateToken() {
        String username = "tokenUser";
        String password = "tokenPassword";
        String email = "token@example.com";

        // Регистрация пользователя
        User user = authService.registerUser(username, password, email);

        // Генерация токена
        String token = jwtService.generateToken(user);
        assertNotNull(token);

        // Валидация токена
        assertTrue(jwtService.validateToken(token));
        assertEquals(username, jwtService.extractUsername(token));
    }

    @Test
    void invalidateToken() {
        String username = "invalidateUser";
        String password = "invalidatePassword";
        String email = "invalidate@example.com";

        // Регистрация пользователя
        User user = authService.registerUser(username, password, email);

        // Генерация токена
        String token = jwtService.generateToken(user);
        assertNotNull(token);

        // Валидация токена до инвалидации
        assertTrue(jwtService.validateToken(token));

        // Инвалидация токена
        jwtService.invalidateToken(token);

        // Проверка токена после инвалидации
        assertFalse(jwtService.validateToken(token));
    }
}
