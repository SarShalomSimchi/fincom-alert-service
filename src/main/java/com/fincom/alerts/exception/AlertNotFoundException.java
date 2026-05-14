package com.fincom.alerts.exception;

public class AlertNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AlertNotFoundException(String alertId) {
        super("Alert not found: " + alertId);
    }
}
