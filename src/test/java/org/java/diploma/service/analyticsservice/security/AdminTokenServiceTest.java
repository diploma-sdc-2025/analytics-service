package org.java.diploma.service.analyticsservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTokenServiceTest {

    private static final String SECRET = "test-test-test-test-test-test-test-test";

    @Test
    void acceptsAdminBearerAndRejectsGuestsAndMalformedHeaders() {
        AdminTokenService service = new AdminTokenService(SECRET, "kon,admin");
        service.init();
        String adminToken = token("kon", false);
        String guestToken = token("kon", true);

        assertThat(service.isAdminBearer("Bearer " + adminToken)).isTrue();
        assertThat(service.isAdminBearer("Bearer " + guestToken)).isFalse();
        assertThat(service.isAdminBearer("Token " + adminToken)).isFalse();
        assertThat(service.isAdminBearer("Bearer   ")).isFalse();
        assertThat(service.isAdminBearer(null)).isFalse();
    }

    @Test
    void checksUsernameMembershipInAdminSet() {
        AdminTokenService service = new AdminTokenService(SECRET, "kon, admin");
        service.init();

        assertThat(service.isAdminToken(token("admin", false))).isTrue();
        assertThat(service.isAdminToken(token("user", false))).isFalse();
        assertThat(service.isAdminToken("not-a-jwt")).isFalse();
    }

    private static String token(String username, boolean guest) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .claim("username", username)
                .claim("guest", guest)
                .signWith(key)
                .compact();
    }
}
