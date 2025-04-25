package io.bomtech.gateway.util; // Adjust package if needed

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    // Make sure this secret is the same as in auth-service and is configured securely
    @Value("${jwt.secret}") // Load secret from application.yml/properties
    private String secretKey;

    public Claims getAllClaimsFromToken(String token) {
        // Consider adding proper exception handling for expired/invalid tokens
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }

    public boolean isTokenExpired(String token) {
        return getAllClaimsFromToken(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            // Basic validation: checks signature and expiration
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Log the exception (e.g., SignatureException, ExpiredJwtException)
            System.err.println("JWT Validation Error: " + e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
         return getAllClaimsFromToken(token).getSubject();
    }
}
