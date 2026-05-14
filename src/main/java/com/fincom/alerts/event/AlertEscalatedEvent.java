package com.fincom.alerts.event;

import com.fincom.alerts.domain.Alert;

import java.time.Instant;

public record AlertEscalatedEvent(
        String event,
        String alertId,
        String tenantId,
        Instant timestamp
) implements AlertEvent {
	
	private static final String EVENT_TYPE = "alert.escalated";
	
    public static AlertEscalatedEvent of(Alert alert) {
        return new AlertEscalatedEvent(EVENT_TYPE, alert.getId(), 
        		alert.getTenantId(), Instant.now());
    }
}
