package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.dto.LeaderboardRowResponse;
import org.java.diploma.service.analyticsservice.dto.PlayerStatsResponse;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final GameplayEventRepository gameplayEventRepository;

    public LeaderboardService(GameplayEventRepository gameplayEventRepository) {
        this.gameplayEventRepository = gameplayEventRepository;
    }

    public List<LeaderboardRowResponse> getLeaderboard(Integer limit) {
        int n = limit == null ? DEFAULT_LIMIT : limit;
        if (n < 1) {
            n = 1;
        }
        if (n > MAX_LIMIT) {
            n = MAX_LIMIT;
        }

        List<Object[]> rows = gameplayEventRepository.findLeaderboardAggregates(n);
        List<LeaderboardRowResponse> out = new ArrayList<>(rows.size());
        int rank = 1;
        for (Object[] r : rows) {
            long userId = ((Number) r[0]).longValue();
            long totalEvents = ((Number) r[1]).longValue();
            long queueJoins = ((Number) r[2]).longValue();
            long queueLeaves = ((Number) r[3]).longValue();
            long matchesPlayed = ((Number) r[4]).longValue();
            double winRatePercent = ((Number) r[5]).doubleValue();
            long currentRating = ((Number) r[6]).longValue();
            out.add(new LeaderboardRowResponse(
                    userId,
                    totalEvents,
                    queueJoins,
                    queueLeaves,
                    matchesPlayed,
                    winRatePercent,
                    currentRating,
                    rank++
            ));
        }
        return out;
    }

    public PlayerStatsResponse getPlayerStats(long userId) {
        List<Object[]> rows = gameplayEventRepository.aggregateStatsForUser(userId);
        if (rows.isEmpty()) {
            return new PlayerStatsResponse(userId, 0, 0, 0);
        }
        Object[] r = rows.get(0);
        long totalEvents = ((Number) r[0]).longValue();
        long queueJoins = ((Number) r[1]).longValue();
        long queueLeaves = ((Number) r[2]).longValue();
        return new PlayerStatsResponse(userId, totalEvents, queueJoins, queueLeaves);
    }
}
