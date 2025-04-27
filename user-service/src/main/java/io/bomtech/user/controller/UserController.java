package io.bomtech.user.controller;

import io.bomtech.user.model.User;
import io.bomtech.user.model.UserSafeDto;
import io.bomtech.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}