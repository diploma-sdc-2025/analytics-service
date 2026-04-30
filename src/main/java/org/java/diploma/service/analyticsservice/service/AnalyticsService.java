package org.java.diploma.service.analyticsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.diploma.service.analyticsservice.dto.LiveEventView;
import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the processing layer of the realtime pipeline:
 * <ul>
 *   <li>persists every event to PostgreSQL ({@code gameplay_events});</li>
 *   <li>maintains rolling windows in Redis sorted sets;</li>
 *   <li>maintains type counters, the active-match set, and the duration histogram;</li>
 *   <li>maintains a capped recent-events ring buffer for the live feed.</li>
 * </ul>
 *
 * <p>All values returned by {@link #getCurrentMetrics()} come from Redis (hot path) so the
 * admin SSE stream never has to touch Postgres.</p>
 */
@Service
public class AnalyticsService {

    private static final String KEY_EVENTS_LAST_MINUTE = "analytics:events:last_minute";
    private static final String KEY_EVENTS_LAST_FIVE_MINUTES = "analytics:events:last_five_minutes";
    private static final String KEY_QUEUE_JOINS_LAST_MINUTE = "analytics:events:queue_join:last_minute";
    private static final String KEY_MATCHES_CREATED_LAST_MINUTE = "analytics:events:match_created:last_minute";
    private static final String KEY_MATCHES_FINISHED_LAST_MINUTE = "analytics:events:match_finished:last_minute";
    private static final String KEY_BATTLES_LAST_MINUTE = "analytics:events:battle_round:last_minute";
    private static final String KEY_PIECES_PURCHASED_LAST_MINUTE = "analytics:events:piece_purchased:last_minute";
    private static final String KEY_CURRENT_QUEUE_SIZE = "analytics:current_queue_size";
    private static final String KEY_TOTAL_QUEUE_JOINS = "analytics:total_queue_joins";
    private static final String KEY_TOTAL_MATCHES_CREATED = "analytics:total_matches_created";
    private static final String KEY_TOTAL_MATCHES_FINISHED = "analytics:total_matches_finished";
    private static final String KEY_LAST_UPDATED = "analytics:last_updated";
    private static final String KEY_EVENT_TYPE_PREFIX = "analytics:event_type:";
    private static final String KEY_LAST_EVENT_JSON = "analytics:last_event_json";
    private static final String KEY_RECENT_EVENTS = "analytics:recent_events";
    private static final String KEY_ACTIVE_MATCHES = "analytics:active_matches";
    private static final String KEY_MATCH_STARTED_AT_PREFIX = "analytics:match_started_at:";
    private static final String KEY_MATCH_DURATIONS_MS = "analytics:match_durations_ms";
    private static final String KEY_MATCH_FINISHED_GUARD_PREFIX = "analytics:match_finished_guard:";

    private static final Duration WINDOW_KEY_TTL = Duration.ofHours(1);
    private static final Duration MATCH_TIMER_TTL = Duration.ofHours(24);
    private static final Duration FINISH_GUARD_TTL = Duration.ofHours(24);
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final long FIVE_MINUTES_MS = 300_000L;
    private static final int RECENT_EVENTS_CAP = 30;
    private static final int MATCH_DURATIONS_CAP = 100;
    private static final int SPARKLINE_SECONDS = 60;

    private static final Set<String> EVENT_TYPES_FOR_BREAKDOWN = Set.of(
            "queue_join", "queue_leave", "match_created", "match_started",
            "battle_round", "piece_purchased", "match_finished"
    );

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private final GameplayEventRepository gameplayEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public AnalyticsService(GameplayEventRepository gameplayEventRepository,
                            RedisTemplate<String, Object> redisTemplate,
                            ObjectMapper objectMapper) {
        this.gameplayEventRepository = gameplayEventRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void processEvent(MatchmakingEvent event) {
        log.info("Processing real-time event: type={}, userId={}, matchId={}",
                event.getType(), event.getUserId(), event.getMatchId());

        saveEventToDatabase(event);
        updateRealTimeMetrics(event);
        updateMatchLifecycle(event);
        appendRecentEvent(event);
    }

    private void saveEventToDatabase(MatchmakingEvent event) {
        try {
            GameplayEvent ge = new GameplayEvent();
            ge.setTime(event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
            ge.setEventType(event.getType());
            ge.setUserId(event.getUserId());
            ge.setMatchId(event.getMatchId());
            ge.setService(serviceForType(event.getType()));
            ge.setMetadata(buildMetadataJson(event));
            gameplayEventRepository.save(ge);
        } catch (RuntimeException e) {
            log.warn("Failed to persist gameplay event type={} userId={}", event.getType(), event.getUserId(), e);
        }
    }

    private void updateRealTimeMetrics(MatchmakingEvent event) {
        long nowMillis = System.currentTimeMillis();
        double score = (double) nowMillis;
        String member = event.getType() + ":" + nowMillis + ":" + UUID.randomUUID();
        String type = event.getType() == null ? "unknown" : event.getType();

        redisTemplate.opsForZSet().add(KEY_EVENTS_LAST_MINUTE, member, score);
        redisTemplate.opsForZSet().add(KEY_EVENTS_LAST_FIVE_MINUTES, member, score);
        redisTemplate.opsForZSet().removeRangeByScore(KEY_EVENTS_LAST_MINUTE, 0, nowMillis - ONE_MINUTE_MS);
        redisTemplate.opsForZSet().removeRangeByScore(KEY_EVENTS_LAST_FIVE_MINUTES, 0, nowMillis - FIVE_MINUTES_MS);

        switch (type) {
            case "queue_join", "player_join" -> trackOneMinuteHit(KEY_QUEUE_JOINS_LAST_MINUTE, member, score, nowMillis,
                    KEY_TOTAL_QUEUE_JOINS);
            case "match_created" -> trackOneMinuteHit(KEY_MATCHES_CREATED_LAST_MINUTE, member, score, nowMillis,
                    KEY_TOTAL_MATCHES_CREATED);
            case "match_finished" -> trackOneMinuteHit(KEY_MATCHES_FINISHED_LAST_MINUTE, member, score, nowMillis,
                    KEY_TOTAL_MATCHES_FINISHED);
            case "battle_round" -> trackOneMinuteHit(KEY_BATTLES_LAST_MINUTE, member, score, nowMillis, null);
            case "piece_purchased" -> trackOneMinuteHit(KEY_PIECES_PURCHASED_LAST_MINUTE, member, score, nowMillis, null);
            default -> {
                // generic event already counted in shared windows
            }
        }

        if (event.getQueueSize() != null && event.getQueueSize() > 0) {
            redisTemplate.opsForValue().set(KEY_CURRENT_QUEUE_SIZE, event.getQueueSize());
        } else if (isQueueShrinkingEvent(type)) {
            redisTemplate.opsForValue().set(KEY_CURRENT_QUEUE_SIZE,
                    event.getQueueSize() == null ? 0 : event.getQueueSize());
        }

        redisTemplate.opsForValue().set(KEY_LAST_UPDATED, Instant.now().toString());
        redisTemplate.opsForValue().increment(KEY_EVENT_TYPE_PREFIX + type);
        redisTemplate.opsForValue().set(KEY_LAST_EVENT_JSON, buildLastEventJson(event));

        redisTemplate.expire(KEY_EVENTS_LAST_MINUTE, WINDOW_KEY_TTL);
        redisTemplate.expire(KEY_EVENTS_LAST_FIVE_MINUTES, WINDOW_KEY_TTL);
    }

    /**
     * Maintains the active-match set and per-match duration timers so the dashboard can show
     * how many games are running and the rolling average duration.
     */
    private void updateMatchLifecycle(MatchmakingEvent event) {
        String type = event.getType();
        Long matchId = event.getMatchId();
        if (type == null || matchId == null) {
            return;
        }
        switch (type) {
            case "match_created" -> {
                redisTemplate.opsForSet().add(KEY_ACTIVE_MATCHES, matchId.toString());
                redisTemplate.expire(KEY_ACTIVE_MATCHES, MATCH_TIMER_TTL);
            }
            case "match_started" -> {
                String key = KEY_MATCH_STARTED_AT_PREFIX + matchId;
                redisTemplate.opsForValue().set(key, Long.toString(System.currentTimeMillis()), MATCH_TIMER_TTL);
            }
            case "match_finished" -> {
                Boolean firstFinish = redisTemplate.opsForValue()
                        .setIfAbsent(KEY_MATCH_FINISHED_GUARD_PREFIX + matchId, "1", FINISH_GUARD_TTL);
                if (Boolean.FALSE.equals(firstFinish)) {
                    return;
                }
                redisTemplate.opsForSet().remove(KEY_ACTIVE_MATCHES, matchId.toString());
                String startedAtRaw = (String) redisTemplate.opsForValue().get(KEY_MATCH_STARTED_AT_PREFIX + matchId);
                if (startedAtRaw != null) {
                    try {
                        long startedAt = Long.parseLong(startedAtRaw);
                        long durationMs = Math.max(0L, System.currentTimeMillis() - startedAt);
                        appendCappedList(KEY_MATCH_DURATIONS_MS, Long.toString(durationMs), MATCH_DURATIONS_CAP);
                    } catch (NumberFormatException ignored) {
                        // skip malformed timer
                    }
                    redisTemplate.delete(KEY_MATCH_STARTED_AT_PREFIX + matchId);
                }
            }
            default -> {
                // not a lifecycle event
            }
        }
    }

    private void appendRecentEvent(MatchmakingEvent event) {
        try {
            String json = objectMapper.writeValueAsString(buildRecentEventEntry(event));
            redisTemplate.opsForList().leftPush(KEY_RECENT_EVENTS, json);
            redisTemplate.opsForList().trim(KEY_RECENT_EVENTS, 0, RECENT_EVENTS_CAP - 1);
            redisTemplate.expire(KEY_RECENT_EVENTS, WINDOW_KEY_TTL);
        } catch (JsonProcessingException e) {
            log.debug("Failed to record recent-events entry", e);
        }
    }

    private Map<String, Object> buildRecentEventEntry(MatchmakingEvent event) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", event.getType() == null ? "unknown" : event.getType());
        entry.put("userMasked", maskUserId(event.getUserId()));
        entry.put("matchId", event.getMatchId());
        entry.put("queueSize", event.getQueueSize() == null ? 0 : event.getQueueSize());
        entry.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : Instant.now().toString());
        entry.put("metadata", maskedMetadata(event.getMetadata()));
        return entry;
    }

    public Map<String, Object> getCurrentMetrics() {
        long nowMillis = System.currentTimeMillis();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("currentQueueSize", parseLong(redisTemplate.opsForValue().get(KEY_CURRENT_QUEUE_SIZE)));
        map.put("totalQueueJoins", parseLong(redisTemplate.opsForValue().get(KEY_TOTAL_QUEUE_JOINS)));
        map.put("totalMatchesCreated", parseLong(redisTemplate.opsForValue().get(KEY_TOTAL_MATCHES_CREATED)));
        map.put("totalMatchesFinished", parseLong(redisTemplate.opsForValue().get(KEY_TOTAL_MATCHES_FINISHED)));

        Object lastUpdatedRaw = redisTemplate.opsForValue().get(KEY_LAST_UPDATED);
        map.put("lastUpdated", lastUpdatedRaw == null ? null : lastUpdatedRaw.toString());

        long eventsLastMinute = sizeWithinWindow(KEY_EVENTS_LAST_MINUTE, nowMillis - ONE_MINUTE_MS, nowMillis);
        long eventsLastFiveMinutes = sizeWithinWindow(KEY_EVENTS_LAST_FIVE_MINUTES, nowMillis - FIVE_MINUTES_MS, nowMillis);
        long queueJoinsLastMinute = sizeWithinWindow(KEY_QUEUE_JOINS_LAST_MINUTE, nowMillis - ONE_MINUTE_MS, nowMillis);
        long matchesCreatedLastMinute = sizeWithinWindow(KEY_MATCHES_CREATED_LAST_MINUTE, nowMillis - ONE_MINUTE_MS, nowMillis);
        long matchesFinishedLastMinute = sizeWithinWindow(KEY_MATCHES_FINISHED_LAST_MINUTE, nowMillis - ONE_MINUTE_MS, nowMillis);
        long battlesLastMinute = sizeWithinWindow(KEY_BATTLES_LAST_MINUTE, nowMillis - ONE_MINUTE_MS, nowMillis);
        long piecesPurchasedLastMinute = sizeWithinWindow(KEY_PIECES_PURCHASED_LAST_MINUTE, nowMillis - ONE_MINUTE_MS, nowMillis);

        double conversionRate = queueJoinsLastMinute > 0
                ? (matchesCreatedLastMinute * 100.0) / queueJoinsLastMinute
                : 0.0;

        map.put("eventsLastMinute", eventsLastMinute);
        map.put("eventsLastFiveMinutes", eventsLastFiveMinutes);
        map.put("queueJoinsLastMinute", queueJoinsLastMinute);
        map.put("matchesCreatedLastMinute", matchesCreatedLastMinute);
        map.put("matchesFinishedLastMinute", matchesFinishedLastMinute);
        map.put("battlesLastMinute", battlesLastMinute);
        map.put("piecesPurchasedLastMinute", piecesPurchasedLastMinute);
        map.put("matchConversionRatePct", Math.round(conversionRate * 10.0) / 10.0);

        long activeMatches = setSize(KEY_ACTIVE_MATCHES);
        long playersInMatches = activeMatches * 2L;
        long currentQueue = parseLong(map.get("currentQueueSize"));
        map.put("activeMatches", activeMatches);
        map.put("playersInMatches", playersInMatches);
        map.put("playersOnline", currentQueue + playersInMatches);
        map.put("averageMatchSeconds", computeAverageMatchSeconds());

        map.put("eventsByType", computeEventsByType());
        map.put("eventsPerSecond", computeEventsPerSecondSeries(nowMillis));
        map.put("recentEvents", readRecentEvents());
        map.put("lastEvent", buildLastEventView((String) redisTemplate.opsForValue().get(KEY_LAST_EVENT_JSON)));

        return map;
    }

    private void trackOneMinuteHit(String key, String member, double score, long nowMillis, String totalCounterKey) {
        redisTemplate.opsForZSet().add(key, member, score);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, nowMillis - ONE_MINUTE_MS);
        redisTemplate.expire(key, WINDOW_KEY_TTL);
        if (totalCounterKey != null) {
            redisTemplate.opsForValue().increment(totalCounterKey);
        }
    }

    private boolean isQueueShrinkingEvent(String type) {
        return "queue_leave".equalsIgnoreCase(type) || "match_created".equalsIgnoreCase(type);
    }

    private String serviceForType(String type) {
        if (type == null) return "unknown";
        return switch (type) {
            case "queue_join", "queue_leave", "player_join", "player_leave", "match_created" -> "matchmaking-service";
            case "match_started", "match_finished", "battle_round", "piece_purchased" -> "game-service";
            default -> "unknown";
        };
    }

    private String buildMetadataJson(MatchmakingEvent event) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (event.getQueueSize() != null) {
            meta.put("queueSize", event.getQueueSize());
        }
        if (event.getTimestamp() != null) {
            meta.put("timestamp", event.getTimestamp().toString());
        }
        if (event.getMetadata() != null) {
            meta.putAll(event.getMetadata());
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private long sizeWithinWindow(String key, long fromInclusive, long toInclusive) {
        Long n = redisTemplate.opsForZSet().count(key, fromInclusive, toInclusive);
        return n == null ? 0L : n;
    }

    private long setSize(String key) {
        Long n = redisTemplate.opsForSet().size(key);
        return n == null ? 0L : n;
    }

    private long parseLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignore) {
            return 0L;
        }
    }

    private double computeAverageMatchSeconds() {
        List<Object> raw = redisTemplate.opsForList().range(KEY_MATCH_DURATIONS_MS, 0, MATCH_DURATIONS_CAP - 1);
        if (raw == null || raw.isEmpty()) return 0.0;
        long sumMs = 0L;
        long count = 0L;
        for (Object o : raw) {
            try {
                sumMs += Long.parseLong(o.toString());
                count++;
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        if (count == 0) return 0.0;
        double avgMs = sumMs / (double) count;
        return Math.round(avgMs / 100.0) / 10.0;
    }

    private Map<String, Long> computeEventsByType() {
        Map<String, Long> out = new LinkedHashMap<>();
        for (String t : EVENT_TYPES_FOR_BREAKDOWN) {
            Object raw = redisTemplate.opsForValue().get(KEY_EVENT_TYPE_PREFIX + t);
            out.put(t, parseLong(raw));
        }
        return out;
    }

    /**
     * One-bin-per-second sparkline of total events for the last {@value SPARKLINE_SECONDS} seconds.
     * Built from the {@code events:last_minute} sorted set so it stays consistent with the rolling counter.
     */
    private List<Long> computeEventsPerSecondSeries(long nowMillis) {
        long start = nowMillis - SPARKLINE_SECONDS * 1000L;
        Set<Object> samples = redisTemplate.opsForZSet().rangeByScore(KEY_EVENTS_LAST_MINUTE, start, nowMillis);
        long[] buckets = new long[SPARKLINE_SECONDS];
        if (samples != null) {
            for (Object sample : samples) {
                if (sample == null) continue;
                String s = sample.toString();
                int firstColon = s.indexOf(':');
                if (firstColon < 0) continue;
                int secondColon = s.indexOf(':', firstColon + 1);
                if (secondColon < 0) continue;
                try {
                    long ts = Long.parseLong(s.substring(firstColon + 1, secondColon));
                    int bucketIdx = (int) ((ts - start) / 1000L);
                    if (bucketIdx >= 0 && bucketIdx < SPARKLINE_SECONDS) {
                        buckets[bucketIdx]++;
                    }
                } catch (NumberFormatException ignored) {
                    // skip non-numeric timestamps
                }
            }
        }
        List<Long> series = new ArrayList<>(SPARKLINE_SECONDS);
        for (long b : buckets) series.add(b);
        return series;
    }

    private List<Map<String, Object>> readRecentEvents() {
        List<Object> raw = redisTemplate.opsForList().range(KEY_RECENT_EVENTS, 0, RECENT_EVENTS_CAP - 1);
        if (raw == null || raw.isEmpty()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Object o : raw) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.readValue(o.toString(), Map.class);
                out.add(parsed);
            } catch (JsonProcessingException ignored) {
                // skip malformed
            }
        }
        out.sort(Comparator.comparing(
                (Map<String, Object> m) -> String.valueOf(m.getOrDefault("timestamp", ""))
        ).reversed());
        return out;
    }

    private void appendCappedList(String key, String value, int cap) {
        redisTemplate.opsForList().leftPush(key, value);
        redisTemplate.opsForList().trim(key, 0, cap - 1);
        redisTemplate.expire(key, WINDOW_KEY_TTL);
    }

    private String buildLastEventJson(MatchmakingEvent event) {
        Map<String, Object> entry = buildRecentEventEntry(event);
        try {
            return objectMapper.writeValueAsString(entry);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private LiveEventView buildLastEventView(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(raw, Map.class);
            String type = String.valueOf(parsed.getOrDefault("type", "unknown"));
            String userMasked = String.valueOf(parsed.getOrDefault("userMasked", "u-****"));
            int queueSize = parseInt(parsed.getOrDefault("queueSize", 0));
            String ts = String.valueOf(parsed.getOrDefault("timestamp", Instant.now().toString()));
            return new LiveEventView(type, userMasked, queueSize, ts);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private int parseInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Map<String, Object> maskedMetadata(Map<String, Object> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : input.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if ("winnerUserId".equals(key) || "loserUserId".equals(key) || key.endsWith("UserId")) {
                out.put(key, maskUserId(value instanceof Number n ? n.longValue() : null));
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    private String maskUserId(Long userId) {
        if (userId == null) return "u-****";
        String s = Long.toString(userId);
        if (s.length() <= 2) return "u-" + s + "**";
        return "u-" + s.charAt(0) + "***" + s.charAt(s.length() - 1);
    }
}
