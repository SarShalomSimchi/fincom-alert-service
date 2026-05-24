package com.fincom.alerts.event;

public interface EventPublisher {
	/**
     * Publishes an alert event.
     * Implementations should handle publishing failures internally and must not
     * throw exceptions that could roll back the alert transaction.
     */
    void publish(AlertEvent event);
}
