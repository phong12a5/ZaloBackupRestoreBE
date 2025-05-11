package io.bomtech.auth.controller;

import io.bomtech.auth.dto.RegisterRequest;
import io.bomtech.auth.model.User;
import io.bomtech.auth.service.AuthService;
import io.bomtech.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Registration error: " + e.getMessage()); // Log lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal server error occurred"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> foundUserOpt = authService.findUserByUsername(user.getUsername());

        if (foundUserOpt.isPresent()) {
            User foundUser = foundUserOpt.get();
            // Kiểm tra mật khẩu (NÊN DÙNG PasswordEncoder)
            // if (passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            if (user.getPassword().equals(foundUser.getPassword())) { // Tạm thời so sánh trực tiếp
                String accessToken = jwtUtil.generateAccessToken(foundUser.getUsername(), foundUser.getRole());
                String refreshToken = jwtUtil.generateRefreshToken(foundUser.getUsername(), foundUser.getRole());
                Map<String, String> tokens = new HashMap<>();
                tokens.put("accessToken", accessToken);
                tokens.put("refreshToken", refreshToken);
                return ResponseEntity.ok(tokens);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is missing"));
        }
        try {
            String username = jwtUtil.validateToken(refreshToken);
            String role = jwtUtil.getRoleFromToken(refreshToken);
            String newAccessToken = jwtUtil.generateAccessToken(username, role);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            System.err.println("Token refresh error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }
    }
}