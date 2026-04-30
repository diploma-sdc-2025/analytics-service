package org.java.diploma.service.analyticsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    GameplayEventRepository gameplayEventRepository;

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @Mock
    ZSetOperations<String, Object> zSetOperations;

    @Mock
    ListOperations<String, Object> listOperations;

    @Mock
    SetOperations<String, Object> setOperations;

    AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
        analyticsService = new AnalyticsService(gameplayEventRepository, redisTemplate, om);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void processEvent_persistsToPostgres_andTracksQueueJoinCounters() {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("queue_join");
        event.setUserId(42L);
        event.setQueueSize(7);
        event.setTimestamp(Instant.now());

        analyticsService.processEvent(event);

        verify(gameplayEventRepository).save(any(GameplayEvent.class));
        verify(valueOperations).increment("analytics:total_queue_joins");
        verify(valueOperations).set("analytics:current_queue_size", 7);
        verify(valueOperations).increment("analytics:event_type:queue_join");
        verify(zSetOperations).add(eq("analytics:events:last_minute"), any(), anyDouble());
        verify(listOperations).leftPush(eq("analytics:recent_events"), any());
    }

    @Test
    void processEvent_matchCreated_addsToActiveMatchSet() {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("match_created");
        event.setUserId(11L);
        event.setMatchId(202L);
        event.setQueueSize(3);
        event.setTimestamp(Instant.now());

        analyticsService.processEvent(event);

        verify(setOperations).add("analytics:active_matches", "202");
        verify(valueOperations).increment("analytics:total_matches_created");
    }

    @Test
    void processEvent_matchFinished_removesFromActiveMatchSet_andRespectsGuard() {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("match_finished");
        event.setUserId(11L);
        event.setMatchId(303L);
        event.setTimestamp(Instant.now());

        when(valueOperations.setIfAbsent(eq("analytics:match_finished_guard:303"), eq("1"), any()))
                .thenReturn(true);

        analyticsService.processEvent(event);

        verify(setOperations).remove("analytics:active_matches", "303");
        verify(valueOperations).increment("analytics:total_matches_finished");
    }

    @Test
    void getCurrentMetrics_returnsAllExpectedKeys_withSafeDefaults() {
        Map<String, Object> metrics = analyticsService.getCurrentMetrics();

        assertNotNull(metrics);
        assertTrue(metrics.containsKey("currentQueueSize"));
        assertTrue(metrics.containsKey("activeMatches"));
        assertTrue(metrics.containsKey("playersInMatches"));
        assertTrue(metrics.containsKey("playersOnline"));
        assertTrue(metrics.containsKey("eventsLastMinute"));
        assertTrue(metrics.containsKey("matchConversionRatePct"));
        assertTrue(metrics.containsKey("eventsByType"));
        assertTrue(metrics.containsKey("eventsPerSecond"));
        assertTrue(metrics.containsKey("recentEvents"));
        assertEquals(0L, metrics.get("currentQueueSize"));
        assertEquals(0L, metrics.get("activeMatches"));
    }
}
