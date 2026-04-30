package org.java.diploma.service.analyticsservice.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java.diploma.service.analyticsservice.config.RedisSubscriber;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.java.diploma.service.analyticsservice.service.AnalyticsWebSocketService;
import org.java.diploma.service.analyticsservice.service.RealtimeAdminStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

    @Mock
    AnalyticsService analyticsService;

    @Mock
    AnalyticsWebSocketService analyticsWebSocketService;

    @Mock
    RealtimeAdminStreamService realtimeAdminStreamService;

    @Mock
    Message message;

    RedisSubscriber redisSubscriber;
    ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        redisSubscriber = new RedisSubscriber(
                objectMapper,
                analyticsService,
                analyticsWebSocketService,
                realtimeAdminStreamService
        );
    }

    @Test
    void onMessage_validJson_dispatchesToProcessor_AndSchedulesBroadcast() throws Exception {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("queue_join");
        event.setUserId(1L);
        event.setQueueSize(3);
        event.setTimestamp(Instant.now());

        when(message.getBody()).thenReturn(objectMapper.writeValueAsBytes(event));

        redisSubscriber.onMessage(message, null);

        verify(analyticsService).processEvent(any(MatchmakingEvent.class));
        verify(realtimeAdminStreamService).scheduleBroadcast();
        verify(analyticsWebSocketService).sendCurrentMetrics();
    }

    @Test
    void onMessage_richMetadataEvent_isAcceptedAndDispatched() throws Exception {
        String body = """
                {
                  "type": "match_finished",
                  "userId": 7,
                  "matchId": 42,
                  "queueSize": 0,
                  "timestamp": "2026-04-29T20:00:00Z",
                  "metadata": { "winnerUserId": 7, "loserUserId": 11 }
                }
                """;
        when(message.getBody()).thenReturn(body.getBytes());

        redisSubscriber.onMessage(message, null);

        verify(analyticsService).processEvent(any(MatchmakingEvent.class));
        verify(realtimeAdminStreamService).scheduleBroadcast();
    }

    @Test
    void onMessage_invalidJson_isSwallowed_andNoDownstreamCalls() {
        when(message.getBody()).thenReturn("not-json".getBytes());

        redisSubscriber.onMessage(message, null);

        verifyNoInteractions(analyticsService);
        verifyNoInteractions(analyticsWebSocketService);
        verifyNoInteractions(realtimeAdminStreamService);
    }
}
