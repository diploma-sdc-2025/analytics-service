package org.java.diploma.service.analyticsservice.event;

import lombok.Data;
import java.time.Instant;

@Data
public class MatchmakingEvent {
    private String type;
    private Long userId;
    private Integer queueSize;
    private Instant timestamp;
}
