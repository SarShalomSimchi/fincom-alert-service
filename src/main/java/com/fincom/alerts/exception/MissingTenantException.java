package com.fincom.alerts.exception;

public class MissingTenantException extends RuntimeException {
    private static final long serialVersionUID = 2647554422464128194L;

	public MissingTenantException() {
        super("X-Tenant-ID header is missing or blank");
    }
}
