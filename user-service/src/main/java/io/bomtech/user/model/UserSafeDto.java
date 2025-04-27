package io.bomtech.user.model;

public class UserSafeDto {
    private String id;
    private String username;
    private String email;
    private String fullname;

    public UserSafeDto(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public UserSafeDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullname = user.getFullname();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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