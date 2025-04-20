// filepath: auth-service/src/main/java/io/bomtech/auth/service/AuthService.java
package io.bomtech.auth.service;

import io.bomtech.auth.model.User;
import io.bomtech.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    public void register(User user) {
        userRepository.save(user);
    }

    public boolean login(User user) {
        return userRepository.findByUsername(user.getUsername())
                .map(u -> u.getPassword().equals(user.getPassword()))
                .orElse(false);
    }
}