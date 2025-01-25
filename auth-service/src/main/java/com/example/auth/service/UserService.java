package com.example.auth.service;

import com.example.auth.exception.UserAlreadyExistsException;
import com.example.auth.exception.UserNotFoundException;
import com.example.auth.model.User;
import com.example.auth.model.UserRoles;
import com.example.auth.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public User registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("User with username " + user.getUsername() + " already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        log.debug("Getting all users");
        return userRepository.findAll();
    }

    public User getUser(Long id) {
        log.debug("Getting user with id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User updateUser(Long id, User updates) {
        log.debug("Updating user with id: {}", id);
        User existingUser = getUser(id);

        if (updates.getUsername() != null) {
            userRepository.findByUsername(updates.getUsername())
                    .filter(user -> !user.getId().equals(id))
                    .ifPresent(user -> {
                        throw new UserAlreadyExistsException("User with username " + updates.getUsername() + " already exists");
                    });
            existingUser.setUsername(updates.getUsername());
        }

        if (updates.getEmail() != null) {
            userRepository.findByEmail(updates.getEmail())
                    .filter(user -> !user.getId().equals(id))
                    .ifPresent(user -> {
                        throw new UserAlreadyExistsException("User with email " + updates.getEmail() + " already exists");
                    });
            existingUser.setEmail(updates.getEmail());
        }

        if (updates.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(updates.getPassword()));
        }

        if (updates.getRoles() != null && !updates.getRoles().isEmpty()) {
            existingUser.setRoles(updates.getRoles());
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);
        User user = getUser(id);
        userRepository.delete(user);
    }

    @PostConstruct
    public void createAdminIfNoneExists() {
        long adminCount = userRepository.countByRoles(UserRoles.ROLE_ADMIN);

        if (adminCount == 0) {
            String encodedPassword = passwordEncoder.encode("defaultPassword");
            User adminUser = User.builder()
                    .username("defaultAdmin")
                    .password(encodedPassword)
                    .roles(Set.of(UserRoles.ROLE_ADMIN))
                    .build();

            userRepository.save(adminUser);
            log.info("Default admin user created with username: defaultAdmin and password: defaultPassword");
        } else {
            log.info("Admin user already exists, skipping creation.");
        }
    }
}
