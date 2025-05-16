package io.bomtech.auth.util;

import io.bomtech.auth.model.User; // Add this import
import io.bomtech.auth.repository.UserRepository; // Add this import
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct; // Import PostConstruct
import org.springframework.beans.factory.annotation.Autowired; // Add this import
import org.springframework.beans.factory.annotation.Value; // Import Value
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    // Remove hardcoded secret key
    // private static final String SECRET_KEY = "y8J3nD9kL2pQ5xR7vT1wZ6mB4aF8oC0sE3uH9gK7tV2qX5r";

    @Value("${jwt.secret}") // Inject secret from properties/yml
    private String secretKeyString;

    @Autowired // Add this annotation to inject UserRepository
    private UserRepository userRepository; // Add this field

    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 30; // 30 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24; // 24 hours

    // Change Key initialization to happen after injection
    private Key key;

    @PostConstruct // Initialize the key after the secret string is injected
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    public String generateAccessToken(String username, String role) { // Pass role directly
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role) // Use the passed role
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, String role) { // Pass role directly
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role) // Use the passed role
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            throw new RuntimeException("Invalid JWT token");
        }
    }

    @SuppressWarnings("deprecation")
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        return (String) claims.get("role");
    }

    public String getUsernameFromToken(String token) {
         Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
         return claims.getSubject();
    }
}