package org.java.diploma.service.analyticsservice.service;

import org.java.diploma.service.analyticsservice.client.AuthRatingClient;
import org.java.diploma.service.analyticsservice.entity.PlayerStatistics;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.repository.PlayerStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerStatisticsUpdaterTest {

    @Mock
    private PlayerStatisticsRepository playerStatisticsRepository;
    @Mock
    private AuthRatingClient authRatingClient;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PlayerStatisticsUpdater updater;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        updater = new PlayerStatisticsUpdater(playerStatisticsRepository, authRatingClient, redisTemplate);
    }

    @Test
    void returnsEarlyWhenEventMetadataMissing() {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setMatchId(1L);

        updater.onMatchFinished(event);

        verify(redisTemplate, never()).opsForValue();
        verify(playerStatisticsRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenGuardAlreadySet() {
        MatchmakingEvent event = finishedEvent(100L, 10L, 20L);
        when(valueOperations.setIfAbsent(eq("analytics:player_stats_applied:100"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        updater.onMatchFinished(event);

        verify(playerStatisticsRepository, never()).save(any());
        verify(authRatingClient, never()).fetchRating(any(Long.class));
    }

    @Test
    void updatesBothPlayersAndRefreshesRatingWhenFirstProcessing() {
        MatchmakingEvent event = finishedEvent(200L, 10L, 20L);
        when(valueOperations.setIfAbsent(eq("analytics:player_stats_applied:200"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        when(playerStatisticsRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(playerStatisticsRepository.findByUserId(20L)).thenReturn(Optional.empty());
        when(authRatingClient.fetchRating(10L)).thenReturn(1210);
        when(authRatingClient.fetchRating(20L)).thenReturn(980);
        when(playerStatisticsRepository.save(any(PlayerStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        updater.onMatchFinished(event);

        ArgumentCaptor<PlayerStatistics> saved = ArgumentCaptor.forClass(PlayerStatistics.class);
        verify(playerStatisticsRepository, org.mockito.Mockito.atLeast(4)).save(saved.capture());
        assertThat(saved.getAllValues()).anySatisfy(ps -> {
            assertThat(ps.getUserId()).isEqualTo(10L);
            assertThat(ps.getCurrentRating()).isEqualTo(1210);
        });
        assertThat(saved.getAllValues()).anySatisfy(ps -> {
            assertThat(ps.getUserId()).isEqualTo(20L);
            assertThat(ps.getCurrentRating()).isEqualTo(980);
        });
    }

    private static MatchmakingEvent finishedEvent(long matchId, long winner, long loser) {
        MatchmakingEvent event = new MatchmakingEvent();
        event.setType("match_finished");
        event.setMatchId(matchId);
        event.setMetadata(Map.of("winnerUserId", winner, "loserUserId", loser));
        return event;
    }
}
