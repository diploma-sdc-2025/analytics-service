package org.java.diploma.service.analyticsservice.repository;

import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GameplayEventRepository extends JpaRepository<GameplayEvent, Long> {

    List<GameplayEvent> findByUserIdOrderByTimeDesc(Long userId);

    List<GameplayEvent> findByEventTypeOrderByTimeDesc(String eventType);

    @Query("SELECT g FROM GameplayEvent g WHERE g.time > :since ORDER BY g.time DESC")
    List<GameplayEvent> findRecentEvents(Instant since);

    @Query("SELECT COUNT(g) FROM GameplayEvent g WHERE g.eventType = :eventType AND g.time > :since")
    Long countByEventTypeAndTimeSince(String eventType, Instant since);

    /**
     * Aggregates persisted matchmaking analytics events per user (player_join / player_leave, etc.).
     */
    @Query(
            value = """
                    SELECT ge.user_id AS uid,
                           COUNT(*)::bigint AS total_events,
                           COALESCE(SUM(CASE WHEN ge.event_type = 'player_join' THEN 1 ELSE 0 END), 0)::bigint AS queue_joins,
                           COALESCE(SUM(CASE WHEN ge.event_type = 'player_leave' THEN 1 ELSE 0 END), 0)::bigint AS queue_leaves,
                           COALESCE(ps.total_matches_played, 0)::bigint AS matches_played,
                           COALESCE(ps.win_rate, 0)::double precision AS win_rate_percent,
                           COALESCE(ps.current_rating, 1000)::bigint AS current_rating
                    FROM gameplay_events ge
                    LEFT JOIN player_statistics ps ON ps.user_id = ge.user_id
                    WHERE ge.user_id IS NOT NULL
                    GROUP BY ge.user_id, ps.total_matches_played, ps.win_rate, ps.current_rating
                    ORDER BY COALESCE(ps.current_rating, 1000) DESC,
                             COALESCE(ps.total_matches_played, 0) DESC,
                             COALESCE(ps.win_rate, 0) DESC,
                             COUNT(*) DESC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<Object[]> findLeaderboardAggregates(@Param("limit") int limit);

    @Query(
            value = """
                    SELECT COALESCE(COUNT(*), 0)::bigint AS total_events,
                           COALESCE(SUM(CASE WHEN event_type = 'player_join' THEN 1 ELSE 0 END), 0)::bigint AS queue_joins,
                           COALESCE(SUM(CASE WHEN event_type = 'player_leave' THEN 1 ELSE 0 END), 0)::bigint AS queue_leaves
                    FROM gameplay_events
                    WHERE user_id = :userId
                    """,
            nativeQuery = true
    )
    List<Object[]> aggregateStatsForUser(@Param("userId") long userId);
}