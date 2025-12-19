package org.java.diploma.service.analyticsservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java.diploma.service.analyticsservice.config.RedisSubscriber;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.java.diploma.service.analyticsservice.service.AnalyticsWebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

    @Mock
    AnalyticsService analyticsService;

    @Mock
    AnalyticsWebSocketService analyticsWebSocketService;

    @Mock
    Message message;

    RedisSubscriber redisSubscriber;

    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        redisSubscriber = new RedisSubscriber(
                objectMapper,
                analyticsService,
                analyticsWebSocketService
        );
    }

    @Test
    void onMessage_validJson_shouldCallServices() throws Exception {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("player_join");
        event.setUserId(1L);
        event.setQueueSize(3);
        event.setTimestamp(Instant.now());

        byte[] body = objectMapper.writeValueAsBytes(event);
        when(message.getBody()).thenReturn(body);

        redisSubscriber.onMessage(message, null);

        verify(analyticsService).processEvent(any());
        verify(analyticsWebSocketService).sendCurrentMetrics();
    }

    @Test
    void onMessage_invalidJson_shouldCatchExceptionAndNotCallServices() {
        when(message.getBody()).thenReturn("invalid-json".getBytes());

        redisSubscriber.onMessage(message, null);

        verifyNoInteractions(analyticsService);
        verifyNoInteractions(analyticsWebSocketService);
    }

}
