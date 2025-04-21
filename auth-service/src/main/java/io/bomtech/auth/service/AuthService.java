// filepath: auth-service/src/main/java/io/bomtech/auth/service/AuthService.java
package io.bomtech.auth.service;

import io.bomtech.auth.model.User;
import io.bomtech.auth.repository.UserRepository;
import io.bomtech.auth.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;
    
    public void register(User user) {
        userRepository.save(user);
    }

    public boolean login(User user) {
        return userRepository.findByUsername(user.getUsername())
        .filter(u -> u.getPassword().equals(user.getPassword()))
        .isPresent();
    }

    public String generateRefreshToken(String username) {
        return jwtUtil.generateRefreshToken(username);
    }
}