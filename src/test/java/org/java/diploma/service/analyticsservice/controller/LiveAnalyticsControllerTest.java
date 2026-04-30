package org.java.diploma.service.analyticsservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class LiveAnalyticsControllerTest {

    @Mock
    AnalyticsService analyticsService;

    LiveAnalyticsController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new LiveAnalyticsController(analyticsService);
    }

    @Test
    void getLiveMetrics_shouldReturnDefaultsWhenRedisEmpty() {
        when(analyticsService.getCurrentMetrics()).thenReturn(Map.of(
                "currentQueueSize", 0,
                "totalQueueJoins", 0
        ));

        Map<String, Object> result = controller.getLiveMetrics();

        assertEquals(0, result.get("currentQueueSize"));
        assertEquals(0, result.get("totalQueueJoins"));
    }

    @Test
    void getLiveMetrics_shouldReturnDefaultsWhenAllValuesNull() {
        when(analyticsService.getCurrentMetrics()).thenReturn(Map.of(
                "currentQueueSize", 0,
                "totalQueueJoins", 0
        ));

        Map<String, Object> result = controller.getLiveMetrics();

        assertEquals(0, result.get("currentQueueSize"));
        assertEquals(0, result.get("totalQueueJoins"));

    }

    @Test
    void getLiveMetrics_shouldReturnActualValuesWhenPresent() {
        when(analyticsService.getCurrentMetrics()).thenReturn(Map.of(
                "currentQueueSize", 5,
                "totalQueueJoins", 10,
                "eventsLastMinute", 3L
        ));

        Map<String, Object> result = controller.getLiveMetrics();

        assertEquals(5, result.get("currentQueueSize"));
        assertEquals(10, result.get("totalQueueJoins"));
        assertEquals(3L, result.get("eventsLastMinute"));
    }

}
