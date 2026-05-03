package org.java.diploma.service.analyticsservice.dto;

public record RecentMatchResponse(
        long opponentUserId,
        String result,
        Integer ratingDelta,
        String playedAt
) {
}
