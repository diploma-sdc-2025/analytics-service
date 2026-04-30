package org.java.diploma.service.analyticsservice.dto;

public record LiveEventView(
        String type,
        String userMasked,
        Integer queueSize,
        String timestamp
) {
}

