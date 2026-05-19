package com.fincom.alerts.api;

import java.util.function.Function;
import java.util.regex.Pattern;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class RequestLoggingAspect {

    private static final String NOT_AVAILABLE = "N/A";
    private static final String MASKED_VALUE = "***";
    private static final Pattern SENSITIVE_QUERY_PARAM_PATTERN =
            Pattern.compile("(?i)(^|&)(authorization|token|password|secret|apikey)=([^&]*)");

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logControllerRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        HttpServletRequest request = currentRequest();

        String httpMethod = safeRequestValue(request, HttpServletRequest::getMethod);
        String uri = safeRequestValue(request, HttpServletRequest::getRequestURI);
        String queryString = request != null ? request.getQueryString() : null;
        String tenantId = safeRequestValue(request, r -> maskTenantId(r.getHeader(ApiHeaders.TENANT_ID)));

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

        return HttpStatus.OK.value(); 
    }

    private String sanitizeQueryString(String queryString) {
        return SENSITIVE_QUERY_PARAM_PATTERN
                .matcher(queryString)
                .replaceAll("$1$2=" + MASKED_VALUE);
    }

    private String maskTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return "missing";
        }

        String trimmed = tenantId.trim();

        if (trimmed.length() <= 4) {
            return MASKED_VALUE;
        }

        return trimmed.substring(0, 2) + MASKED_VALUE + trimmed.substring(trimmed.length() - 2);
    }

    private String safeRequestValue(HttpServletRequest request, Function<HttpServletRequest, String> extractor) {
        return request == null ? NOT_AVAILABLE : extractor.apply(request);
    }
}