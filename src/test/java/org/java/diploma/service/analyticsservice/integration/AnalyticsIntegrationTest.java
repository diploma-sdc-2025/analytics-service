package org.java.diploma.service.analyticsservice.integration;

import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = RedisReactiveAutoConfiguration.class)
class AnalyticsIntegrationTest {

    @Autowired
    AnalyticsService analyticsService;

    @Autowired
    GameplayEventRepository gameplayEventRepository;

    @MockitoBean
    RedisMessageListenerContainer redisMessageListenerContainer;

    @MockitoBean
    RedisConnectionFactory redisConnectionFactory;

    @MockitoBean
    RedisTemplate<String, Object> redisTemplate;

    @Test
    void processEvent_shouldPersistGameplayEvent() {
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        ZSetOperations<String, Object> zSetOps = mock(ZSetOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);

        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("player_join");
        event.setUserId(99L);
        event.setQueueSize(5);
        event.setTimestamp(Instant.now());

        analyticsService.processEvent(event);

        List<GameplayEvent> events =
                gameplayEventRepository.findByUserIdOrderByTimeDesc(99L);

        assertEquals(1, events.size());
        assertEquals("player_join", events.getFirst().getEventType());
    }
}
