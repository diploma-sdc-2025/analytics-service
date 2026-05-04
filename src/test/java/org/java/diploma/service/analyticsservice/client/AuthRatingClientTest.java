package org.java.diploma.service.analyticsservice.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRatingClientTest {

    @Test
    void returnsDefaultWhenInternalSecretBlank() {
        AuthRatingClient client = new AuthRatingClient(mock(RestClient.class), " ");

        assertThat(client.fetchRating(10L)).isEqualTo(1000);
    }

    @Test
    void returnsRatingFromAuthServiceBody() {
        RestClient restClient = mock(RestClient.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(eq("/api/internal/users/{userId}/rating"), eq(11L))).thenReturn(headersSpec);
        when(headersSpec.header(eq("X-Internal-Secret"), any(String[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("rating", 1337));
        AuthRatingClient client = new AuthRatingClient(restClient, "secret");

        assertThat(client.fetchRating(11L)).isEqualTo(1337);
    }

    @Test
    void returnsDefaultOnRestError() {
        RestClient restClient = mock(RestClient.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        @SuppressWarnings("rawtypes")
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(eq("/api/internal/users/{userId}/rating"), eq(9L))).thenReturn(headersSpec);
        when(headersSpec.header(eq("X-Internal-Secret"), any(String[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RestClientException("down"));
        AuthRatingClient client = new AuthRatingClient(restClient, "secret");

        assertThat(client.fetchRating(9L)).isEqualTo(1000);
    }
}
