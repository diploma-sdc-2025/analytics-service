package org.java.diploma.service.analyticsservice.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics/live")
public class LiveAnalyticsController {

    private final RedisTemplate<String, Object> redisTemplate;

    public LiveAnalyticsController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    public Map<String, Object> getLiveMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Object queueSize = redisTemplate.opsForValue().get("analytics:current_queue_size");
        metrics.put("currentQueueSize", queueSize != null ? queueSize : 0);

        Object totalJoins = redisTemplate.opsForValue().get("analytics:total_queue_joins");
        metrics.put("totalQueueJoins", totalJoins != null ? totalJoins : 0);

        Long eventsLastMinute = redisTemplate.opsForZSet().zCard("analytics:events:last_minute");
        metrics.put("eventsLastMinute", eventsLastMinute != null ? eventsLastMinute : 0);

        Object lastUpdated = redisTemplate.opsForValue().get("analytics:last_updated");
        metrics.put("lastUpdated", lastUpdated);

        Object playerJoins = redisTemplate.opsForValue().get("analytics:event_type:player_join");
        Object playerLeaves = redisTemplate.opsForValue().get("analytics:event_type:player_leave");

        Map<String, Object> eventsByType = new HashMap<>();
        eventsByType.put("player_join", playerJoins != null ? playerJoins : 0);
        eventsByType.put("player_leave", playerLeaves != null ? playerLeaves : 0);
        metrics.put("eventsByType", eventsByType);

        return metrics;
    }
}
