package org.java.diploma.service.analyticsservice.dto;

/**
 * Per-user stats from {@code gameplay_events} (Redis/matchmaking pipeline).
 * Usernames are resolved by auth-service on the client.
 */
public record LeaderboardRowResponse(
        long userId,
        long totalEvents,
        long queueJoins,
        long queueLeaves,
        long matchesPlayed,
        double winRatePercent,
        long currentRating,
        int rank
) {
}
