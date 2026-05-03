package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.client.AuthRatingClient;
import org.java.diploma.service.analyticsservice.dto.LeaderboardRowResponse;
import org.java.diploma.service.analyticsservice.dto.PlayerStatsResponse;
import org.java.diploma.service.analyticsservice.dto.RecentMatchResponse;
import org.java.diploma.service.analyticsservice.entity.PlayerStatistics;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.java.diploma.service.analyticsservice.repository.PlayerStatisticsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LeaderboardService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int RECENT_MATCHES_CAP = 8;

    private final GameplayEventRepository gameplayEventRepository;
    private final PlayerStatisticsRepository playerStatisticsRepository;
    private final AuthRatingClient authRatingClient;

    public LeaderboardService(
            GameplayEventRepository gameplayEventRepository,
            PlayerStatisticsRepository playerStatisticsRepository,
            AuthRatingClient authRatingClient
    ) {
        this.gameplayEventRepository = gameplayEventRepository;
        this.playerStatisticsRepository = playerStatisticsRepository;
        this.authRatingClient = authRatingClient;
    }

    public List<LeaderboardRowResponse> getLeaderboard(Integer limit) {
        int n = limit == null ? DEFAULT_LIMIT : limit;
        if (n < 1) {
            n = 1;
        }
        if (n > MAX_LIMIT) {
            n = MAX_LIMIT;
        }

        /*
         * SQL ORDER BY uses analytics.player_statistics.current_rating, which often lags auth-service.
         * Fetch a wider pool, attach live rating from auth, then sort and take the top {@code n} so
         * high-rated accounts are not stuck below seed/demo rows tied at 1000 in {@code player_statistics}.
         */
        int poolSize = Math.min(Math.max(n * 40, 100), 500);
        List<Object[]> rows = gameplayEventRepository.findLeaderboardAggregates(poolSize);
        List<LeaderboardRowResponse> scored = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long userId = ((Number) r[0]).longValue();
            long totalEvents = ((Number) r[1]).longValue();
            long queueJoins = ((Number) r[2]).longValue();
            long queueLeaves = ((Number) r[3]).longValue();
            long matchesPlayed = ((Number) r[4]).longValue();
            double winRatePercent = ((Number) r[5]).doubleValue();
            long liveRating = authRatingClient.fetchRating(userId);
            scored.add(new LeaderboardRowResponse(
                    userId,
                    totalEvents,
                    queueJoins,
                    queueLeaves,
                    matchesPlayed,
                    winRatePercent,
                    liveRating,
                    0
            ));
        }
        scored.sort((a, b) -> {
            int c = Long.compare(b.currentRating(), a.currentRating());
            if (c != 0) {
                return c;
            }
            c = Long.compare(b.matchesPlayed(), a.matchesPlayed());
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.winRatePercent(), a.winRatePercent());
            if (c != 0) {
                return c;
            }
            c = Long.compare(b.totalEvents(), a.totalEvents());
            if (c != 0) {
                return c;
            }
            return Long.compare(a.userId(), b.userId());
        });
        int take = Math.min(n, scored.size());
        List<LeaderboardRowResponse> out = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            LeaderboardRowResponse row = scored.get(i);
            out.add(new LeaderboardRowResponse(
                    row.userId(),
                    row.totalEvents(),
                    row.queueJoins(),
                    row.queueLeaves(),
                    row.matchesPlayed(),
                    row.winRatePercent(),
                    row.currentRating(),
                    i + 1
            ));
        }
        return out;
    }

    public PlayerStatsResponse getPlayerStats(long userId) {
        List<Object[]> queueRows = gameplayEventRepository.aggregateStatsForUser(userId);
        long totalEvents = 0;
        long queueJoins = 0;
        long queueLeaves = 0;
        if (!queueRows.isEmpty()) {
            Object[] q = queueRows.get(0);
            totalEvents = ((Number) q[0]).longValue();
            queueJoins = ((Number) q[1]).longValue();
            queueLeaves = ((Number) q[2]).longValue();
        }

        Optional<PlayerStatistics> psOpt = playerStatisticsRepository.findByUserId(userId);
        long matchesPlayed = psOpt.map(p -> (long) p.getTotalMatchesPlayed()).orElse(0L);
        long wins = psOpt.map(p -> (long) p.getTotalMatchesWon()).orElse(0L);
        long losses = Math.max(0L, matchesPlayed - wins);

        double winRatePercent = 0.0;
        if (matchesPlayed > 0) {
            winRatePercent = Math.round((wins * 100.0 / matchesPlayed) * 10.0) / 10.0;
        }

        long currentRating = authRatingClient.fetchRating(userId);

        List<RecentMatchResponse> recentMatches = buildRecentMatches(userId);

        return new PlayerStatsResponse(
                userId,
                totalEvents,
                queueJoins,
                queueLeaves,
                matchesPlayed,
                wins,
                losses,
                winRatePercent,
                currentRating,
                recentMatches
        );
    }

    private List<RecentMatchResponse> buildRecentMatches(long userId) {
        List<Object[]> rows = gameplayEventRepository.findRecentFinishedMatches(userId, RECENT_MATCHES_CAP);
        List<RecentMatchResponse> out = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            if (row.length < 3) {
                continue;
            }
            String playedAt = toIso(row[0]);
            long opponentId = ((Number) row[1]).longValue();
            String outcome = row[2] != null ? row[2].toString() : "-";
            out.add(new RecentMatchResponse(opponentId, outcome, null, playedAt));
        }
        return out;
    }

    private static String toIso(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.sql.Timestamp ts) {
            return ts.toInstant().toString();
        }
        if (raw instanceof Instant i) {
            return i.toString();
        }
        return raw.toString();
    }
}
