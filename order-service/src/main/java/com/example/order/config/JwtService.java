package com.example.order.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        String username = claims.getSubject();
        log.debug("Extracted username from token: {}", username);
        return username;
    }


    public Long getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        log.info(String.valueOf(claims));
        Number userIdNumber = (Number) claims.get("userId");
        log.debug("Extracted userId from token: {}", userIdNumber);
        return userIdNumber.longValue();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        List<String> roles = (List<String>) claims.get("roles");
        log.debug("Extracted roles from token: {}", roles);
        return roles;
    }


    public boolean validateToken(String token) {
        try {
            log.debug("Validating JWT token");
            Claims claims = getClaims(token);
            Date expirationDate = claims.getExpiration();
            boolean isValid = !expirationDate.before(new Date());
            log.info("JWT token validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Error validating JWT token", e);
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
