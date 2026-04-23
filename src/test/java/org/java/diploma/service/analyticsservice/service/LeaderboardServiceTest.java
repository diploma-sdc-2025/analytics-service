package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.dto.LeaderboardRowResponse;
import org.java.diploma.service.analyticsservice.dto.PlayerStatsResponse;
import org.java.diploma.service.analyticsservice.repository.GameplayEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private GameplayEventRepository repository;

    @InjectMocks
    private LeaderboardService service;

    @Test
    void getLeaderboardUsesDefaultLimitWhenNull() {
        when(repository.findLeaderboardAggregates(50)).thenReturn(List.of(
                new Object[]{1L, 20L, 11L, 9L},
                new Object[]{2L, 15L, 8L, 7L}
        ));

        List<LeaderboardRowResponse> rows = service.getLeaderboard(null);

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).rank());
        assertEquals(2, rows.get(1).rank());
        assertEquals(20L, rows.get(0).totalEvents());
        verify(repository).findLeaderboardAggregates(50);
    }

    @Test
    void getLeaderboardClampsLimitRange() {
        when(repository.findLeaderboardAggregates(1)).thenReturn(List.of());
        when(repository.findLeaderboardAggregates(100)).thenReturn(List.of());

        service.getLeaderboard(0);
        service.getLeaderboard(999);

        verify(repository).findLeaderboardAggregates(1);
        verify(repository).findLeaderboardAggregates(100);
    }

    @Test
    void getPlayerStatsReturnsZeroesWhenNoRows() {
        when(repository.aggregateStatsForUser(7L)).thenReturn(List.of());

        PlayerStatsResponse stats = service.getPlayerStats(7L);

        assertEquals(7L, stats.userId());
        assertEquals(0L, stats.totalEvents());
        assertEquals(0L, stats.queueJoins());
        assertEquals(0L, stats.queueLeaves());
    }

    @Test
    void getPlayerStatsMapsAggregateRow() {
        when(repository.aggregateStatsForUser(9L)).thenReturn(List.<Object[]>of(new Object[]{31L, 16L, 15L}));

        PlayerStatsResponse stats = service.getPlayerStats(9L);

        assertEquals(9L, stats.userId());
        assertEquals(31L, stats.totalEvents());
        assertEquals(16L, stats.queueJoins());
        assertEquals(15L, stats.queueLeaves());
    }
}
