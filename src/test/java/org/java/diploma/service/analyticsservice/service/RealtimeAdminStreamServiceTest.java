package org.java.diploma.service.analyticsservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeAdminStreamServiceTest {

    @Test
    void subscribeAddsEmitterAndTracksActiveClients() {
        AnalyticsService analyticsService = mock(AnalyticsService.class);
        RealtimeAdminStreamService service = new RealtimeAdminStreamService(analyticsService);

        SseEmitter emitter = service.subscribe();

        assertThat(emitter).isNotNull();
        assertThat(service.activeClientCount()).isEqualTo(1);
        emitter.complete();
    }

    @Test
    void flushPendingBroadcastSendsSnapshotWhenScheduled() {
        AnalyticsService analyticsService = mock(AnalyticsService.class);
        when(analyticsService.getCurrentMetrics()).thenReturn(Map.of("onlinePlayers", 5));
        RealtimeAdminStreamService service = new RealtimeAdminStreamService(analyticsService);
        service.subscribe();
        service.scheduleBroadcast();

        service.flushPendingBroadcast();

        verify(analyticsService).getCurrentMetrics();
        assertThat(service.getLastBroadcastAtMs()).isGreaterThan(0L);
    }

    @Test
    void flushPendingBroadcastSkipsWhenNoSubscribers() {
        AnalyticsService analyticsService = mock(AnalyticsService.class);
        RealtimeAdminStreamService service = new RealtimeAdminStreamService(analyticsService);
        service.scheduleBroadcast();

        service.flushPendingBroadcast();

        verify(analyticsService, org.mockito.Mockito.never()).getCurrentMetrics();
    }

    @Test
    void heartbeatNoopsWhenNoSubscribers() {
        AnalyticsService analyticsService = mock(AnalyticsService.class);
        RealtimeAdminStreamService service = new RealtimeAdminStreamService(analyticsService);

        service.sendHeartbeat();

        verify(analyticsService, org.mockito.Mockito.never()).getCurrentMetrics();
    }
}
