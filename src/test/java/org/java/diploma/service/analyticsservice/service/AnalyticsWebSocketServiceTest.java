package org.java.diploma.service.analyticsservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsWebSocketServiceTest {

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    AnalyticsService analyticsService;

    @InjectMocks
    AnalyticsWebSocketService analyticsWebSocketService;

    @Test
    void sendCurrentMetrics_shouldSendToTopic() {
        when(analyticsService.getCurrentMetrics())
                .thenReturn(Map.of("currentQueueSize", 5));

        analyticsWebSocketService.sendCurrentMetrics();

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/analytics"), eq(Map.of("currentQueueSize", 5)));
    }
}
