package com.fincom.alerts.event;

import com.fincom.alerts.domain.Alert;
import com.fincom.alerts.domain.AlertStatus;

import java.time.Instant;

public record AlertDecidedEvent(
        String event,
        String alertId,
        String tenantId,
        Instant timestamp,
        AlertStatus decision
) implements AlertEvent {
	
	private static final String EVENT_TYPE = "alert.decided";
	
    public static AlertDecidedEvent of(Alert alert) {
        return new AlertDecidedEvent(EVENT_TYPE, alert.getId(), 
        		alert.getTenantId(), Instant.now(), 
        		alert.getStatus());
    }
}
