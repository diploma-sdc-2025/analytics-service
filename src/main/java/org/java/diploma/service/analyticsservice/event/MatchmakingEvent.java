package org.java.diploma.service.analyticsservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;

/**
 * Wire format published on Redis Pub/Sub channel {@code analytics:events}. Despite the legacy class name,
 * this DTO carries every analytics event flowing into the stream (matchmaking, gameplay, economy).
 *
 * <p>{@code metadata} is an optional bag for event-specific payload (e.g. {@code centipawns},
 * {@code piece}, {@code winnerUserId}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchmakingEvent {
    private String type;
    private Long userId;
    private Long matchId;
    private Integer queueSize;
    private Instant timestamp;
    private Map<String, Object> metadata;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
