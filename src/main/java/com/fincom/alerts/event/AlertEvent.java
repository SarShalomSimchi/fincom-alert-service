package com.fincom.alerts.event;

import java.time.Instant;

public interface AlertEvent {
    String event();
    String alertId();
    String tenantId();
    Instant timestamp();
}
