package com.example.auth.service;

import com.example.auth.exception.UserAlreadyExistsException;
import com.example.auth.exception.UserNotFoundException;
import com.example.auth.model.User;
import com.example.auth.model.UserRoles;
import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setEmail("test@example.com");
        testUser.setRoles(Set.of(UserRoles.ROLE_USER));
    }

    @Test
    void registerUser_Success() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("password123");
        user.setEmail("newuser@example.com");
        user.setRoles(Set.of(UserRoles.ROLE_USER));

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(user.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        User registeredUser = userService.registerUser(user);

        assertNotNull(registeredUser);
        assertEquals("newuser", registeredUser.getUsername());
        assertEquals("encodedPassword", registeredUser.getPassword());
        assertEquals("newuser@example.com", registeredUser.getEmail());
        assertTrue(registeredUser.getRoles().contains(UserRoles.ROLE_USER));

        verify(userRepository, times(1)).findByUsername("newuser");
        verify(userRepository, times(1)).findByEmail("newuser@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void registerUser_UsernameExists() {
        User user = new User();
        user.setUsername("existinguser");
        user.setPassword("password123");
        user.setEmail("newemail@example.com");

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(new User()));

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(user));

        assertEquals("User with username existinguser already exists", exception.getMessage());

        verify(userRepository, times(1)).findByUsername("existinguser");
        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_EmailExists() {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("password123");
        user.setEmail("existing@example.com");

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(new User()));

        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(user));

        assertEquals("User with email existing@example.com already exists", exception.getMessage());

        verify(userRepository, times(1)).findByUsername("newuser");
        verify(userRepository, times(1)).findByEmail("existing@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAllUsers_Success() {
        List<User> expectedUsers = Arrays.asList(
            testUser,
            User.builder()
                .id(2L)
                .username("user2")
                .email("user2@example.com")
                .roles(Set.of(UserRoles.ROLE_USER))
                .build()
        );

        when(userRepository.findAll()).thenReturn(expectedUsers);

        List<User> actualUsers = userService.getAllUsers();

        assertEquals(2, actualUsers.size());
        assertEquals(expectedUsers, actualUsers);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User foundUser = userService.getUser(1L);

        assertNotNull(foundUser);
        assertEquals(testUser.getId(), foundUser.getId());
        assertEquals(testUser.getUsername(), foundUser.getUsername());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(999L));
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void updateUser_Success() {
        User updates = new User();
        updates.setUsername("updateduser");
        updates.setEmail("updated@example.com");
        updates.setPassword("newpassword");
        updates.setRoles(Set.of(UserRoles.ROLE_USER, UserRoles.ROLE_ADMIN));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(updates.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(updates.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(updates.getPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateUser(1L, updates);

        assertNotNull(updatedUser);
        assertEquals(updates.getUsername(), updatedUser.getUsername());
        assertEquals(updates.getEmail(), updatedUser.getEmail());
        assertEquals("encodedNewPassword", updatedUser.getPassword());
        assertEquals(updates.getRoles(), updatedUser.getRoles());

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findByUsername(updates.getUsername());
        verify(userRepository, times(1)).findByEmail(updates.getEmail());
        verify(passwordEncoder, times(1)).encode(updates.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_NotFound() {
        User updates = new User();
        updates.setUsername("updateduser");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(999L, updates));
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_UsernameExists() {
        User updates = new User();
        updates.setUsername("existinguser");

        User existingUser = new User();
        existingUser.setId(2L);
        existingUser.setUsername("existinguser");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername(updates.getUsername())).thenReturn(Optional.of(existingUser));

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUser(1L, updates));
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findByUsername(updates.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).delete(testUser);

        userService.deleteUser(1L);

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(testUser);
    }

    @Test
    void deleteUser_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(999L));
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void createAdminIfNoneExists_AdminDoesNotExist() {
        when(userRepository.countByRoles(UserRoles.ROLE_ADMIN)).thenReturn(0L);
        when(passwordEncoder.encode("defaultPassword")).thenReturn("encodedDefaultPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.createAdminIfNoneExists();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).countByRoles(UserRoles.ROLE_ADMIN);
        verify(passwordEncoder, times(1)).encode("defaultPassword");
        verify(userRepository, times(1)).save(userCaptor.capture());

        User adminUser = userCaptor.getValue();
        assertEquals("defaultAdmin", adminUser.getUsername());
        assertEquals("encodedDefaultPassword", adminUser.getPassword());
        assertTrue(adminUser.getRoles().contains(UserRoles.ROLE_ADMIN));
    }

    @Test
    void createAdminIfNoneExists_AdminExists() {
        when(userRepository.countByRoles(UserRoles.ROLE_ADMIN)).thenReturn(1L);

        userService.createAdminIfNoneExists();

        verify(userRepository, times(1)).countByRoles(UserRoles.ROLE_ADMIN);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
