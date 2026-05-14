package com.fincom.alerts.exception;

public class MissingTenantException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MissingTenantException() {
        super("X-Tenant-ID header is missing or blank");
    }
}
