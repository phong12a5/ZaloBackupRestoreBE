// filepath: user-service/src/main/java/io/bomtech/user/service/UserService.java
package io.bomtech.user.service;

import io.bomtech.user.model.User;
import io.bomtech.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User getUserById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }
}