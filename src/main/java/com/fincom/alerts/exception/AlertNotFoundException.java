package com.fincom.alerts.exception;

public class AlertNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -1342024100980881231L;

	public AlertNotFoundException(String alertId) {
        super("Alert not found: " + alertId);
    }
}
