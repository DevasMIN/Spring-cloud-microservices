package com.example.auth.repository;

import com.example.auth.model.User;
import com.example.auth.model.UserRoles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    long countByRoles(UserRoles role);
}
