package com.fincom.alerts.api;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fincom.alerts.exception.MissingTenantException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class TenantArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return String.class.equals(parameter.getParameterType()) && parameter.hasParameterAnnotation(TenantId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String tenantId = null;
        if (request != null) {
            tenantId = request.getHeader(TENANT_HEADER);
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantException();
        }
        return tenantId.trim();
    }
}
