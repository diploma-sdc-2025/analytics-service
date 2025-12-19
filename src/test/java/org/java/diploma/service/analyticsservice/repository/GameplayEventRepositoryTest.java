package org.java.diploma.service.analyticsservice.repository;

import org.java.diploma.service.analyticsservice.entity.GameplayEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@ActiveProfiles("test")
class GameplayEventRepositoryTest {

    @Autowired
    GameplayEventRepository repository;

    @Test
    void findRecentEvents_shouldReturnEventsAfterTimestamp() {
        GameplayEvent event = new GameplayEvent();
        event.setEventType("player_join");
        event.setService("MATCHMAKING");
        event.setTime(Instant.now());

        repository.save(event);

        List<GameplayEvent> events =
                repository.findRecentEvents(Instant.now().minusSeconds(60));

        assertFalse(events.isEmpty());
    }
}
