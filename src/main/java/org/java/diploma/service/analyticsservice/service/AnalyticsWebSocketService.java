package org.java.diploma.service.analyticsservice.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AnalyticsService analyticsService;

    public AnalyticsWebSocketService(SimpMessagingTemplate messagingTemplate,
                                     AnalyticsService analyticsService) {
        this.messagingTemplate = messagingTemplate;
        this.analyticsService = analyticsService;
    }

    public void sendCurrentMetrics() {
        var metrics = analyticsService.getCurrentMetrics();
        messagingTemplate.convertAndSend("/topic/analytics", metrics);
    }
}
