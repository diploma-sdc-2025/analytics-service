package org.java.diploma.service.analyticsservice.repository;

import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}