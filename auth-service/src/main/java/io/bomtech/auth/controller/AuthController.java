package io.bomtech.auth.controller;

import io.bomtech.auth.dto.RegisterRequest; // Import the new DTO
import io.bomtech.auth.model.User;
import io.bomtech.auth.service.AuthService;
import io.bomtech.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Import HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional; // Import Optional

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
            // Trả về 200 OK nếu đăng ký thành công
            return ResponseEntity.ok(Map.of("message", "User registered successfully"));
        } catch (IllegalArgumentException e) {
            // Nếu username đã tồn tại (AuthService ném IllegalArgumentException)
            // Trả về 400 Bad Request với thông báo lỗi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // Xử lý các lỗi không mong muốn khác
            System.err.println("Registration error: " + e.getMessage()); // Log lỗi
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An internal server error occurred"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        // Tìm user theo username
        Optional<User> foundUserOpt = authService.findUserByUsername(user.getUsername());

        if (foundUserOpt.isPresent()) {
            User foundUser = foundUserOpt.get();
            // Kiểm tra mật khẩu (NÊN DÙNG PasswordEncoder)
            // if (passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            if (user.getPassword().equals(foundUser.getPassword())) { // Tạm thời so sánh trực tiếp
                String accessToken = jwtUtil.generateAccessToken(foundUser.getUsername());
                String refreshToken = jwtUtil.generateRefreshToken(foundUser.getUsername());
                Map<String, String> tokens = new HashMap<>();
                tokens.put("accessToken", accessToken);
                tokens.put("refreshToken", refreshToken);
                return ResponseEntity.ok(tokens);
            }
        }
        // Nếu không tìm thấy user hoặc sai mật khẩu
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is missing"));
        }
        try {
            // validateToken trả về username nếu hợp lệ, ném exception nếu không
            String username = jwtUtil.validateToken(refreshToken);
            // Nếu không có exception, token hợp lệ và username đã được lấy
            String newAccessToken = jwtUtil.generateAccessToken(username);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) { // Bắt exception từ validateToken (bao gồm cả RuntimeException từ JwtUtil)
            System.err.println("Token refresh error: " + e.getMessage()); // Log lỗi
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }
    }
}