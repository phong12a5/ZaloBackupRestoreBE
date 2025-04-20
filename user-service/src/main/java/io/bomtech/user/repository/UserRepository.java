// filepath: user-service/src/main/java/io/bomtech/user/repository/UserRepository.java
package io.bomtech.user.repository;

import io.bomtech.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}