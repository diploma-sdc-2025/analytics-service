package org.java.diploma.service.analyticsservice.controller;

import org.java.diploma.service.analyticsservice.security.AdminTokenService;
import org.java.diploma.service.analyticsservice.service.AnalyticsService;
import org.java.diploma.service.analyticsservice.service.RealtimeAdminStreamService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics/admin")
public class AdminRealtimeAnalyticsController {

    private final AdminTokenService adminTokenService;
    private final AnalyticsService analyticsService;
    private final RealtimeAdminStreamService realtimeAdminStreamService;

    public AdminRealtimeAnalyticsController(
            AdminTokenService adminTokenService,
            AnalyticsService analyticsService,
            RealtimeAdminStreamService realtimeAdminStreamService
    ) {
        this.adminTokenService = adminTokenService;
        this.analyticsService = analyticsService;
        this.realtimeAdminStreamService = realtimeAdminStreamService;
    }

    @GetMapping("/live")
    public Map<String, Object> adminLive(@RequestHeader(value = "Authorization", required = false) String authorization) {
        requireAdminBearer(authorization);
        return analyticsService.getCurrentMetrics();
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam("token") String token) {
        if (!adminTokenService.isAdminToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin token required");
        }
        SseEmitter emitter = realtimeAdminStreamService.subscribe();
        try {
            emitter.send(SseEmitter.event().name("metrics").data(analyticsService.getCurrentMetrics()));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private void requireAdminBearer(String authorization) {
        if (!adminTokenService.isAdminBearer(authorization)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin token required");
        }
    }
}

