package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.client.AuthRatingClient;
import org.java.diploma.service.analyticsservice.dto.LeaderboardRowResponse;
import org.java.diploma.service.analyticsservice.dto.PlayerStatsResponse;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.java.diploma.service.analyticsservice.repository.PlayerStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private GameplayEventRepository repository;

    @Mock
    private PlayerStatisticsRepository playerStatisticsRepository;

    @Mock
    private AuthRatingClient authRatingClient;

    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(repository, playerStatisticsRepository, authRatingClient);
    }

    @Test
    void getLeaderboardUsesDefaultLimitWhenNull() {
        // Columns: userId, totalEvents, queueJoins, queueLeaves, matchesPlayed, winRatePct, currentRating
        when(repository.findLeaderboardAggregates(anyInt())).thenReturn(List.of(
                new Object[]{1L, 20L, 11L, 9L, 12L, 58.3, 1450L},
                new Object[]{2L, 15L, 8L, 7L, 5L, 20.0, 1100L}
        ));
        when(authRatingClient.fetchRating(1L)).thenReturn(1450);
        when(authRatingClient.fetchRating(2L)).thenReturn(1100);

        List<LeaderboardRowResponse> rows = service.getLeaderboard(null);

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).rank());
        assertEquals(2, rows.get(1).rank());
        assertEquals(1L, rows.get(0).userId());
        assertEquals(1450L, rows.get(0).currentRating());
        assertEquals(20L, rows.get(0).totalEvents());
        verify(repository).findLeaderboardAggregates(500);
    }

    @Test
    void getLeaderboardClampsLimitRange() {
        when(repository.findLeaderboardAggregates(anyInt())).thenReturn(List.of());

        service.getLeaderboard(0);
        service.getLeaderboard(999);

        verify(repository).findLeaderboardAggregates(100);
        verify(repository).findLeaderboardAggregates(500);
    }

    @Test
    void getPlayerStatsReturnsZeroesWhenNoRows() {
        when(repository.aggregateStatsForUser(7L)).thenReturn(List.of());
        when(playerStatisticsRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(authRatingClient.fetchRating(7L)).thenReturn(1000);
        when(repository.findRecentFinishedMatches(7L, 8)).thenReturn(List.of());

        PlayerStatsResponse stats = service.getPlayerStats(7L);

        assertEquals(7L, stats.userId());
        assertEquals(0L, stats.totalEvents());
        assertEquals(0L, stats.queueJoins());
        assertEquals(0L, stats.queueLeaves());
        assertEquals(0L, stats.matchesPlayed());
        assertEquals(1000L, stats.currentRating());
    }

    @Test
    void getPlayerStatsMapsAggregateRow() {
        when(repository.aggregateStatsForUser(9L)).thenReturn(List.<Object[]>of(new Object[]{31L, 16L, 15L}));
        when(playerStatisticsRepository.findByUserId(9L)).thenReturn(Optional.empty());
        when(authRatingClient.fetchRating(9L)).thenReturn(1200);
        when(repository.findRecentFinishedMatches(9L, 8)).thenReturn(List.of());

        PlayerStatsResponse stats = service.getPlayerStats(9L);

        assertEquals(9L, stats.userId());
        assertEquals(31L, stats.totalEvents());
        assertEquals(16L, stats.queueJoins());
        assertEquals(15L, stats.queueLeaves());
        assertEquals(1200L, stats.currentRating());
    }
}
