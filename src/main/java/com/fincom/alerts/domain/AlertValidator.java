package com.fincom.alerts.domain;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.fincom.alerts.exception.AlertAlreadyDecidedException;
import com.fincom.alerts.exception.InvalidTransitionException;

@Component
public class AlertValidator {
	
	private static final Set<AlertStatus> DECIDABLE = Set.of(AlertStatus.OPEN, AlertStatus.ESCALATED);

    public void validateEscalate(Alert alert) {
        if (alert == null) {
            throw new InvalidTransitionException("Alert must not be null");
        }
        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new InvalidTransitionException(
            		"Alert can only be escalated when status is OPEN. Current status: " + alert.getStatus());
        }
    }

    public void validateDecide(Alert alert) {
        if (alert == null) {
            throw new InvalidTransitionException("Alert must not be null");
        }
        if (!DECIDABLE.contains(alert.getStatus())) {
            throw new AlertAlreadyDecidedException(
            		"Alert has already been decided. Current status: " + alert.getStatus());
        }
    }
}
