// filepath: user-service/src/main/java/io/bomtech/user/controller/UserController.java
package io.bomtech.user.controller;

import io.bomtech.user.model.User;
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
}