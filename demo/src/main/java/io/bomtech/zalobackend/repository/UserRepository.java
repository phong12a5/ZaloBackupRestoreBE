package io.bomtech.zalobackend.repository;

import io.bomtech.zalobackend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
}
