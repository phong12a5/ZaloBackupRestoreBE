package io.bomtech.auth.service;

import io.bomtech.auth.dto.RegisterRequest; // Import the DTO
import io.bomtech.auth.model.User;
import io.bomtech.auth.repository.UserRepository;
import io.bomtech.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map; // Import Map
import java.util.HashMap; // Import HashMap
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebClient.Builder webClientBuilder;

    // Modify register method to accept RegisterRequest
    public void register(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        // Check if email already exists (optional, but recommended)
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        // IMPORTANT: Encode the password in a real application!
        // newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setPassword(registerRequest.getPassword()); // Temporary direct set
        newUser.setEmail(registerRequest.getEmail());

        newUser.setRole("USER");

        User savedUser = userRepository.save(newUser);

        Map<String, String> userServiceData = new HashMap<>();
        userServiceData.put("id", savedUser.getId());
        userServiceData.put("username", savedUser.getUsername());
        userServiceData.put("email", savedUser.getEmail());
        userServiceData.put("fullname", registerRequest.getFullname());

        webClientBuilder.build()
                .post()
                .uri("lb://user-service/users")
                .body(Mono.just(userServiceData), Map.class)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> System.err.println("Failed to send user to user-service: " + error.getMessage()))
                .subscribe();
    }

    public boolean login(User user) {
        return userRepository.findByUsername(user.getUsername())
        .filter(u -> u.getPassword().equals(user.getPassword()))
        .isPresent();
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}