package org.java.diploma.service.analyticsservice.dto;

import java.util.List;

public record PlayerStatsResponse(
        long userId,
        long totalEvents,
        long queueJoins,
        long queueLeaves,
        long matchesPlayed,
        long wins,
        long losses,
        double winRatePercent,
        long currentRating,
        List<RecentMatchResponse> recentMatches
) {
}
