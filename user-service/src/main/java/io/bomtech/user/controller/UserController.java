package io.bomtech.user.controller;

import io.bomtech.user.model.User;
import io.bomtech.user.model.UserSafeDto;
import io.bomtech.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("X-User-Name") String username) {
        return userService.getUserByUsername(username)
            .map(user -> {
                return ResponseEntity.ok(new UserSafeDto(user));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(@RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Access denied");
        }
        List<UserSafeDto> users = userService.getAllUsersSafe();
        return ResponseEntity.ok(users);
    }
}