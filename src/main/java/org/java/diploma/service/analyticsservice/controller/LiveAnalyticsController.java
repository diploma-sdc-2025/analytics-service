package org.java.diploma.service.analyticsservice.controller;

import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics/live")
public class LiveAnalyticsController {

    private final AnalyticsService analyticsService;

    public LiveAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public Map<String, Object> getLiveMetrics() {
        return analyticsService.getCurrentMetrics();
    }
}
