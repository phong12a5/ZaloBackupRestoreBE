// filepath: user-service/src/main/java/io/bomtech/user/service/UserService.java
package io.bomtech.user.service;

import io.bomtech.user.model.User;
import io.bomtech.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(User user) {
        // Add any validation or business logic here if needed
        return userRepository.save(user);
    }
}