package com.fincom.alerts.exception;

public class InvalidTransitionException extends RuntimeException {
    private static final long serialVersionUID = -5306373015174418909L;

    public InvalidTransitionException(String message) {
        super(message);
    }
}