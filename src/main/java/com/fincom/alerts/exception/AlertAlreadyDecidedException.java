package com.fincom.alerts.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlertAlreadyDecidedException extends RuntimeException {
    private static final long serialVersionUID = -7738054853612613253L;

    public AlertAlreadyDecidedException(String message) {
        super(message);
    }
}