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
                    SELECT user_id AS uid,
                           COUNT(*)::bigint AS total_events,
                           COALESCE(SUM(CASE WHEN event_type = 'player_join' THEN 1 ELSE 0 END), 0)::bigint AS queue_joins,
                           COALESCE(SUM(CASE WHEN event_type = 'player_leave' THEN 1 ELSE 0 END), 0)::bigint AS queue_leaves
                    FROM gameplay_events
                    WHERE user_id IS NOT NULL
                    GROUP BY user_id
                    ORDER BY COUNT(*) DESC,
                             SUM(CASE WHEN event_type = 'player_join' THEN 1 ELSE 0 END) DESC
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