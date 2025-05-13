package io.bomtech.user.service;

import io.bomtech.user.model.User;
import io.bomtech.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.bomtech.user.model.UserSafeDto; // Add this import
import java.util.List; // Add this import
import java.util.stream.Collectors; // Add this import

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
        return userRepository.save(user);
    }

    public List<UserSafeDto> getAllUsersSafe() {
        return userRepository.findAll()
                .stream()
                .map(UserSafeDto::new)
                .collect(Collectors.toList());
    }
}