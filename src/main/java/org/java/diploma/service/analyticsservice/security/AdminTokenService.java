package org.java.diploma.service.analyticsservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminTokenService {

    private final String jwtSecret;
    private final String adminUsersCsv;
    private SecretKey key;
    private Set<String> adminUsers;

    public AdminTokenService(
            @Value("${auth.jwt.secret}") String jwtSecret,
            @Value("${analytics.admin.users:kon}") String adminUsersCsv
    ) {
        this.jwtSecret = jwtSecret;
        this.adminUsersCsv = adminUsersCsv;
    }

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.adminUsers = Arrays.stream(adminUsersCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean isAdminBearer(String authorizationHeader) {
        String token = extractBearer(authorizationHeader);
        if (token == null) {
            return false;
        }
        return isAdminToken(token);
    }

    public boolean isAdminToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object guest = claims.get("guest");
            if (Boolean.TRUE.equals(guest)) {
                return false;
            }
            String username = claims.get("username", String.class);
            return username != null && adminUsers.contains(username.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private String extractBearer(String authorizationHeader) {
        if (authorizationHeader == null) return null;
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) return null;
        String token = authorizationHeader.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }
}

