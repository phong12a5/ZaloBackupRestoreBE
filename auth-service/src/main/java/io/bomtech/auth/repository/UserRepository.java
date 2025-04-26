// filepath: auth-service/src/main/java/io/bomtech/auth/repository/UserRepository.java
package io.bomtech.auth.repository;

import io.bomtech.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // Add method to find by email
}