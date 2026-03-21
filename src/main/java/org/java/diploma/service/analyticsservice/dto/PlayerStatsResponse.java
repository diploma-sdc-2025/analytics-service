package org.java.diploma.service.analyticsservice.dto;

public record PlayerStatsResponse(
        long userId,
        long totalEvents,
        long queueJoins,
        long queueLeaves
) {
}
