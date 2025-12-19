package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    AnalyticsService analyticsService;

    @Test
    void processEvent_shouldSaveEventAndUpdateMetrics() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("player_join");
        event.setUserId(42L);
        event.setQueueSize(7);
        event.setTimestamp(Instant.now());

        analyticsService.processEvent(event);

        verify(gameplayEventRepository).save(any(GameplayEvent.class));
        verify(valueOperations).increment("analytics:total_queue_joins");
        verify(valueOperations).set("analytics:current_queue_size", 7);
        verify(valueOperations).increment("analytics:event_type:player_join");
        verify(zSetOperations).add(eq("analytics:events:last_minute"), any(), anyDouble());
    }

    @Test
    void getCurrentMetrics_shouldReplaceNullsWithZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(any()))
                .thenReturn(Arrays.asList(null, null, null));

        Map<String, Object> metrics = analyticsService.getCurrentMetrics();

        assertEquals(0, metrics.get("analytics:current_queue_size"));
        assertEquals(0, metrics.get("analytics:total_queue_joins"));
    }

    @Test
    void getCurrentMetrics_shouldReturnActualValues() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(any()))
                .thenReturn(List.of(5, 10, "now"));

        Map<String, Object> metrics = analyticsService.getCurrentMetrics();

        assertEquals(5, metrics.get("analytics:current_queue_size"));
        assertEquals(10, metrics.get("analytics:total_queue_joins"));
    }
}
