package org.java.diploma.service.analyticsservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.java.diploma.service.analyticsservice.service.AnalyticsWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisSubscriber.class);
    private final ObjectMapper objectMapper;
    private final AnalyticsService analyticsService;
    private final AnalyticsWebSocketService analyticsWebSocketService;

    public RedisSubscriber(ObjectMapper objectMapper,
                           AnalyticsService analyticsService,
                           AnalyticsWebSocketService analyticsWebSocketService) {
        this.objectMapper = objectMapper;
        this.analyticsService = analyticsService;
        this.analyticsWebSocketService = analyticsWebSocketService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            log.info("Received real-time event: {}", messageBody);

            MatchmakingEvent event = objectMapper.readValue(messageBody, MatchmakingEvent.class);

            analyticsService.processEvent(event);

            analyticsWebSocketService.sendCurrentMetrics();

        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }
}
