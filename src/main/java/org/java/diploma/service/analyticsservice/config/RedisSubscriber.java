package org.java.diploma.service.analyticsservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java.diploma.service.analyticsservice.event.MatchmakingEvent;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.java.diploma.service.analyticsservice.service.AnalyticsWebSocketService;
import org.java.diploma.service.analyticsservice.service.RealtimeAdminStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Ingestion-layer entry point. Decodes a JSON event from {@code analytics:events} and dispatches it to:
 * <ul>
 *   <li>{@link AnalyticsService#processEvent(MatchmakingEvent)} for persistence + Redis aggregates;</li>
 *   <li>{@link RealtimeAdminStreamService#scheduleBroadcast()} so the admin SSE stream coalesces fan-out;</li>
 *   <li>{@link AnalyticsWebSocketService#sendCurrentMetrics()} as the optional STOMP delivery channel.</li>
 * </ul>
 *
 * <p>Per-event errors are logged and swallowed so a single bad payload never breaks the stream.</p>
 */
@Component
public class RedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisSubscriber.class);
    private final ObjectMapper objectMapper;
    private final AnalyticsService analyticsService;
    private final AnalyticsWebSocketService analyticsWebSocketService;
    private final RealtimeAdminStreamService realtimeAdminStreamService;

    public RedisSubscriber(ObjectMapper objectMapper,
                           AnalyticsService analyticsService,
                           AnalyticsWebSocketService analyticsWebSocketService,
                           RealtimeAdminStreamService realtimeAdminStreamService) {
        this.objectMapper = objectMapper;
        this.analyticsService = analyticsService;
        this.analyticsWebSocketService = analyticsWebSocketService;
        this.realtimeAdminStreamService = realtimeAdminStreamService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            log.debug("Received realtime event: {}", body);

            MatchmakingEvent event = objectMapper.readValue(body, MatchmakingEvent.class);
            analyticsService.processEvent(event);

            // Coalesced SSE fan-out (primary delivery channel).
            realtimeAdminStreamService.scheduleBroadcast();

            // Optional STOMP fan-out kept best-effort.
            try {
                analyticsWebSocketService.sendCurrentMetrics();
            } catch (RuntimeException stompFailure) {
                log.debug("STOMP broadcast skipped", stompFailure);
            }
        } catch (Exception e) {
            log.error("Error processing realtime event", e);
        }
    }
}
