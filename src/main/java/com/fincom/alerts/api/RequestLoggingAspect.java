package com.fincom.alerts.api;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequestLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingAspect.class);

    @Around("execution(* com.fincom.alerts.api.*.*(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("→ {}.{}", joinPoint.getTarget().getClass().getSimpleName(), 
                             joinPoint.getSignature().getName());
        try {
            Object result = joinPoint.proceed();
            log.info("← {}.{}", joinPoint.getTarget().getClass().getSimpleName(),
                                 joinPoint.getSignature().getName());
            return result;
        } catch (Exception ex) {
            log.error("✗ {}.{} threw {}", joinPoint.getTarget().getClass().getSimpleName(),
                                           joinPoint.getSignature().getName(),
                                           ex.getMessage());
            throw ex;
        }
    }
}