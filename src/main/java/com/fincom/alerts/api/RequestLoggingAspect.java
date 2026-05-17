package com.fincom.alerts.api;

import jakarta.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingAspect.class);

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logControllerRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        HttpServletRequest request = currentRequest();

        String httpMethod = request != null ? request.getMethod() : "N/A";
        String uri = request != null ? request.getRequestURI() : "N/A";
        String queryString = request != null ? request.getQueryString() : null;
        String tenantId = request != null ? maskTenantId(request.getHeader("X-Tenant-ID")) : "N/A";

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        String safeRequestTarget = queryString == null ? uri : uri + "?" + sanitizeQueryString(queryString);

        log.info("Incoming request: method={}, path={}, handler={}.{}, tenant={}",
                httpMethod,
                safeRequestTarget,
                className,
                methodName,
                tenantId);

        try {
            Object result = joinPoint.proceed();

            long durationMs = System.currentTimeMillis() - startTime;
            int status = extractStatus(result);

            log.info("Completed request: method={}, path={}, handler={}.{}, status={}, durationMs={}",
                    httpMethod,
                    uri,
                    className,
                    methodName,
                    status,
                    durationMs);

            return result;
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startTime;

            log.warn("Failed request: method={}, path={}, handler={}.{}, error={}, durationMs={}",
                    httpMethod,
                    uri,
                    className,
                    methodName,
                    ex.getClass().getSimpleName(),
                    durationMs);

            throw ex;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes != null ? attributes.getRequest() : null;
    }

    private int extractStatus(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }

        return 200;
    }

    private String sanitizeQueryString(String queryString) {
        return queryString
                .replaceAll("(?i)(authorization|token|password|secret|apiKey|apikey)=([^&]*)", "$1=***");
    }

    private String maskTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "missing";
        }

        String trimmed = tenantId.trim();

        if (trimmed.length() <= 4) {
            return "***";
        }

        return trimmed.substring(0, 2) + "***" + trimmed.substring(trimmed.length() - 2);
    }
}