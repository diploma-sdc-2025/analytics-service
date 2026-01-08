package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private final GameplayEventRepository gameplayEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public AnalyticsService(GameplayEventRepository gameplayEventRepository,
                            RedisTemplate<String, Object> redisTemplate) {
        this.gameplayEventRepository = gameplayEventRepository;
        this.redisTemplate = redisTemplate;
    }

    public void processEvent(MatchmakingEvent event) {
        log.info("Processing real-time event: type={}, userId={}, queueSize={}",
                event.getType(), event.getUserId(), event.getQueueSize());

        saveEventToDatabase(event);

        updateRealTimeMetrics(event);

        log.info("Event processed in real-time");
    }

    private void saveEventToDatabase(MatchmakingEvent event) {
        GameplayEvent gameplayEvent = new GameplayEvent();

        gameplayEvent.setTime(event.getTimestamp());
        gameplayEvent.setEventType(event.getType());
        gameplayEvent.setUserId(event.getUserId());
        gameplayEvent.setMatchId(null);
        gameplayEvent.setService("MATCHMAKING_SERVICE");

        String metadata = String.format(
                "{\"queueSize\": %d, \"timestamp\": \"%s\"}",
                event.getQueueSize(),
                event.getTimestamp().toString()
        );
        gameplayEvent.setMetadata(metadata);

        gameplayEventRepository.save(gameplayEvent);
        log.info("Event saved to database with ID: {}", gameplayEvent.getId());
    }

    private void updateRealTimeMetrics(MatchmakingEvent event) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        redisTemplate.opsForZSet().add("analytics:events:last_minute", timestamp, Double.parseDouble(timestamp));

        long oneMinuteAgo = System.currentTimeMillis() - 60000;
        redisTemplate.opsForZSet().removeRangeByScore("analytics:events:last_minute", 0, oneMinuteAgo);

        redisTemplate.opsForValue().increment("analytics:total_queue_joins");
        redisTemplate.opsForValue().set("analytics:current_queue_size", event.getQueueSize());
        redisTemplate.opsForValue().set("analytics:last_updated", Instant.now().toString());

        redisTemplate.opsForValue().increment("analytics:event_type:" + event.getType());

        redisTemplate.expire("analytics:events:last_minute", Duration.ofHours(1));

        log.info("Redis metrics updated");
    }

    public Map<String, Object> getCurrentMetrics() {
        List<String> keys = List.of(
                "analytics:current_queue_size",
                "analytics:total_queue_joins",
                "analytics:last_updated"
        );

        List<Object> values = redisTemplate.opsForValue().multiGet(keys);

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            assert values != null;
            Object value = values.get(i);
            map.put(keys.get(i), value != null ? value : 0);
        }

        return map;
    }
}