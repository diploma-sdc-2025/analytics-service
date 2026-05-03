package org.java.diploma.service.analyticsservice.client;

import org.java.diploma.service.analyticsservice.config.AuthServiceClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class AuthRatingClient {

    private static final Logger log = LoggerFactory.getLogger(AuthRatingClient.class);

    private final RestClient authRestClient;
    private final String internalSecret;

    public AuthRatingClient(
            @Qualifier(AuthServiceClientConfig.AUTH_REST_CLIENT) RestClient authRestClient,
            @Value("${diploma.internal-api.secret:local-dev-internal-secret}") String internalSecret
    ) {
        this.authRestClient = authRestClient;
        this.internalSecret = internalSecret;
    }

    /**
     * Reads current rating from auth-service (same DB updated when game finishes with ±10).
     */
    public int fetchRating(long userId) {
        if (internalSecret == null || internalSecret.isBlank()) {
            return 1000;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = authRestClient.get()
                    .uri("/api/internal/users/{userId}/rating", userId)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                return 1000;
            }
            Object r = body.get("rating");
            if (r instanceof Number n) {
                return n.intValue();
            }
            return 1000;
        } catch (RestClientException e) {
            log.warn("Could not fetch rating for userId={}: {}", userId, e.getMessage());
            return 1000;
        }
    }
}
