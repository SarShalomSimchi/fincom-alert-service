package com.fincom.alerts.event;

public interface EventPublisher {
    void publish(AlertEvent event);
}
