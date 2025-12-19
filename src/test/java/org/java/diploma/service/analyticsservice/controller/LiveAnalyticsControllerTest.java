package org.java.diploma.service.analyticsservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class LiveAnalyticsControllerTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @Mock
    ZSetOperations<String, Object> zSetOperations;

    LiveAnalyticsController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        controller = new LiveAnalyticsController(redisTemplate);
    }

    @Test
    void getLiveMetrics_shouldReturnDefaultsWhenRedisEmpty() {
        when(valueOperations.get("analytics:current_queue_size")).thenReturn(null);
        when(valueOperations.get("analytics:total_queue_joins")).thenReturn(null);
        when(zSetOperations.zCard("analytics:events:last_minute")).thenReturn(null);

        Map<String, Object> result = controller.getLiveMetrics();

        assertEquals(0, result.get("currentQueueSize"));
        assertEquals(0, result.get("totalQueueJoins"));
    }

    @Test
    void getLiveMetrics_shouldReturnDefaultsWhenAllValuesNull() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(zSetOperations.zCard(anyString())).thenReturn(null);

        Map<String, Object> result = controller.getLiveMetrics();

        assertEquals(0, result.get("currentQueueSize"));
        assertEquals(0, result.get("totalQueueJoins"));

        Map<?, ?> eventsByType = (Map<?, ?>) result.get("eventsByType");
        assertEquals(0, eventsByType.get("player_join"));
        assertEquals(0, eventsByType.get("player_leave"));
    }

    @Test
    void getLiveMetrics_shouldReturnActualValuesWhenPresent() {
        when(valueOperations.get("analytics:current_queue_size")).thenReturn(5);
        when(valueOperations.get("analytics:total_queue_joins")).thenReturn(10);
        when(zSetOperations.zCard("analytics:events:last_minute")).thenReturn(3L);

        Map<String, Object> result = controller.getLiveMetrics();

        assertEquals(5, result.get("currentQueueSize"));
        assertEquals(10, result.get("totalQueueJoins"));
        assertEquals(3L, result.get("eventsLastMinute"));
    }

}
