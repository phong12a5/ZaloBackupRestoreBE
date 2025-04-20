// filepath: user-service/src/main/java/io/bomtech/user/model/User.java
package io.bomtech.user.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String email;

    // Getters and Setters
}
