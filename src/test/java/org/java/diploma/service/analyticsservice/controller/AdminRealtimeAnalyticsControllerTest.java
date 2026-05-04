package org.java.diploma.service.analyticsservice.controller;

import org.java.diploma.service.analyticsservice.security.AdminTokenService;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.java.diploma.service.analyticsservice.service.RealtimeAdminStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRealtimeAnalyticsControllerTest {

    @Mock
    private AdminTokenService adminTokenService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private RealtimeAdminStreamService realtimeAdminStreamService;

    private AdminRealtimeAnalyticsController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminRealtimeAnalyticsController(adminTokenService, analyticsService, realtimeAdminStreamService);
    }

    @Test
    void adminLiveReturnsMetricsForAdminBearer() {
        when(adminTokenService.isAdminBearer("Bearer ok")).thenReturn(true);
        when(analyticsService.getCurrentMetrics()).thenReturn(Map.of("usersOnline", 7));

        Map<String, Object> body = controller.adminLive("Bearer ok");

        assertThat(body).containsEntry("usersOnline", 7);
        verify(analyticsService).getCurrentMetrics();
    }

    @Test
    void adminLiveRejectsNonAdminBearer() {
        when(adminTokenService.isAdminBearer("Bearer nope")).thenReturn(false);

        assertThatThrownBy(() -> controller.adminLive("Bearer nope"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void streamRejectsInvalidAdminToken() {
        when(adminTokenService.isAdminToken("bad")).thenReturn(false);

        assertThatThrownBy(() -> controller.stream("bad"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void streamSubscribesAndReturnsEmitterForValidToken() {
        when(adminTokenService.isAdminToken("good")).thenReturn(true);
        SseEmitter emitter = new SseEmitter();
        when(realtimeAdminStreamService.subscribe()).thenReturn(emitter);
        when(analyticsService.getCurrentMetrics()).thenReturn(Map.of("snapshot", 1));

        SseEmitter result = controller.stream("good");

        assertThat(result).isSameAs(emitter);
        verify(realtimeAdminStreamService).subscribe();
    }
}
