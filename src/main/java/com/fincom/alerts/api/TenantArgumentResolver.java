package com.fincom.alerts.api;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fincom.alerts.exception.MissingTenantException;

@Component
public class TenantArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return String.class.equals(parameter.getParameterType()) && parameter.hasParameterAnnotation(TenantId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String tenantId = webRequest.getHeader(ApiHeaders.TENANT_ID);

        if (tenantId == null || tenantId.isBlank()) {
            throw new MissingTenantException();
        }

        return tenantId.trim();
    }
}
