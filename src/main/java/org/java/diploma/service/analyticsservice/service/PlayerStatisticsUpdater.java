package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.client.AuthRatingClient;
import org.java.diploma.service.analyticsservice.entity.PlayerStatistics;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.repository.PlayerStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;

/**
 * Keeps {@code player_statistics} aligned with finished matches and pulls fresh ratings from auth-service.
 */
@Service
public class PlayerStatisticsUpdater {

    private static final Logger log = LoggerFactory.getLogger(PlayerStatisticsUpdater.class);
    private static final String STATS_GUARD_PREFIX = "analytics:player_stats_applied:";
    private static final Duration STATS_GUARD_TTL = Duration.ofHours(72);

    private final PlayerStatisticsRepository playerStatistics;
    private final AuthRatingClient authRatingClient;
    private final RedisTemplate<String, Object> redisTemplate;

    public PlayerStatisticsUpdater(
            PlayerStatisticsRepository playerStatistics,
            AuthRatingClient authRatingClient,
            RedisTemplate<String, Object> redisTemplate
    ) {
        this.playerStatistics = playerStatistics;
        this.authRatingClient = authRatingClient;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void onMatchFinished(MatchmakingEvent event) {
        Long matchId = event.getMatchId();
        Map<String, Object> meta = event.getMetadata();
        if (matchId == null || meta == null) {
            return;
        }
        Long winnerId = toLong(meta.get("winnerUserId"));
        Long loserId = toLong(meta.get("loserUserId"));
        if (winnerId == null || loserId == null || winnerId.equals(loserId)) {
            log.debug("match_finished missing winner/loser in metadata matchId={}", matchId);
            return;
        }

        String guardKey = STATS_GUARD_PREFIX + matchId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(guardKey, "1", STATS_GUARD_TTL);
        if (Boolean.FALSE.equals(first)) {
            return;
        }

        applyWinLoss(winnerId, true);
        applyWinLoss(loserId, false);

        refreshRatingFromAuth(winnerId);
        refreshRatingFromAuth(loserId);
    }

    private void applyWinLoss(long userId, boolean won) {
        PlayerStatistics row = playerStatistics.findByUserId(userId).orElseGet(() -> newRow(userId));
        row.setTotalMatchesPlayed(row.getTotalMatchesPlayed() + 1);
        if (won) {
            row.setTotalMatchesWon(row.getTotalMatchesWon() + 1);
        }
        row.recomputeWinRate();
        saveRow(row);
    }

    private void refreshRatingFromAuth(long userId) {
        PlayerStatistics row = playerStatistics.findByUserId(userId).orElseGet(() -> newRow(userId));
        row.setCurrentRating(authRatingClient.fetchRating(userId));
        saveRow(row);
    }

    private PlayerStatistics newRow(long userId) {
        PlayerStatistics ps = new PlayerStatistics();
        ps.setUserId(userId);
        return ps;
    }

    private void saveRow(PlayerStatistics row) {
        try {
            playerStatistics.save(row);
        } catch (DataIntegrityViolationException dup) {
            PlayerStatistics existing = playerStatistics.findByUserId(row.getUserId()).orElseThrow();
            existing.setTotalMatchesPlayed(row.getTotalMatchesPlayed());
            existing.setTotalMatchesWon(row.getTotalMatchesWon());
            existing.recomputeWinRate();
            existing.setCurrentRating(row.getCurrentRating());
            playerStatistics.save(existing);
        }
    }

    private static Long toLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
