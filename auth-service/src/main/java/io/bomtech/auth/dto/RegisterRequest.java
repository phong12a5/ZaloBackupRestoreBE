package io.bomtech.auth.dto;

// Add validation imports if needed, e.g.:
// import jakarta.validation.constraints.Email;
// import jakarta.validation.constraints.NotBlank;

public class RegisterRequest {

    // @NotBlank // Add validation if using spring-boot-starter-validation
    private String username;

    // @NotBlank
    private String password;

    // @NotBlank
    // @Email
    private String email;

    // @NotBlank
    private String fullname;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
}
