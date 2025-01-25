package com.example.auth.controller;

import com.example.auth.exception.UserNotFoundException;
import com.example.auth.model.User;
import com.example.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.debug("Getting all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        log.debug("Getting user with id: {}", id);
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User updates) {
        log.debug("Updating user with id: {}", id);
        return ResponseEntity.ok(userService.updateUser(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) throws UserNotFoundException {
        log.debug("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
