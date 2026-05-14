package com.fincom.alerts.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.api.dto.DecisionRequest;
import com.fincom.alerts.config.WebConfig;
import com.fincom.alerts.domain.AlertStatus;
import com.fincom.alerts.exception.AlertAlreadyDecidedException;
import com.fincom.alerts.exception.AlertNotFoundException;
import com.fincom.alerts.exception.InvalidTransitionException;
import com.fincom.alerts.exception.MissingTenantException;
import com.fincom.alerts.service.AlertService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AlertController.class)
@ExtendWith(SpringExtension.class)
@Import({WebConfig.class, GlobalExceptionHandler.class})
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @MockitoBean
    private TenantArgumentResolver tenantArgumentResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AlertResponse sampleResponse = new AlertResponse("a1", "tx1", "ent", 50, AlertStatus.OPEN, null, "tenant-1", null, null, null);

    @BeforeEach
    void setup() throws Exception {
        when(tenantArgumentResolver.supportsParameter(any(org.springframework.core.MethodParameter.class))).thenReturn(true);
        when(tenantArgumentResolver.resolveArgument(
                any(org.springframework.core.MethodParameter.class),
                any(org.springframework.web.method.support.ModelAndViewContainer.class),
                any(org.springframework.web.context.request.NativeWebRequest.class),
                any(org.springframework.web.bind.support.WebDataBinderFactory.class)
        )).thenReturn("tenant-1");
    }

    @Test
    void givenValidRequest_whenPostCreate_thenReturns201() throws Exception {
        CreateAlertRequest req = new CreateAlertRequest("tx1", "ent", 50, null);
        when(alertService.create(any(CreateAlertRequest.class), eq("tenant-1"))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(sampleResponse)));
    }

    @Test
    void givenBlankTransactionId_whenPostCreate_thenReturns400() throws Exception {
        CreateAlertRequest req = new CreateAlertRequest("", "ent", 50, null);

        mockMvc.perform(post("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenInvalidMatchScore_whenPostCreate_thenReturns400() throws Exception {
        String body = "{\"transactionId\":\"tx\",\"matchedEntityName\":\"e\",\"matchScore\":200}";

        mockMvc.perform(post("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenMissingTenantHeader_whenPostCreate_thenReturns400() throws Exception {
        CreateAlertRequest req = new CreateAlertRequest("tx1", "ent", 50, null);
        when(tenantArgumentResolver.resolveArgument(
                any(org.springframework.core.MethodParameter.class),
                any(org.springframework.web.method.support.ModelAndViewContainer.class),
                any(org.springframework.web.context.request.NativeWebRequest.class),
                any(org.springframework.web.bind.support.WebDataBinderFactory.class)
        )).thenThrow(new MissingTenantException());

        mockMvc.perform(post("/api/v1/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAlertsExist_whenGetList_thenReturns200WithList() throws Exception {
        when(alertService.list("tenant-1", null, null)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/alerts").header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(sampleResponse))));
    }

    @Test
    void givenInvalidMinMatchScore_whenGetList_thenReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/alerts?minMatchScore=abc").header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenValidDecision_whenPatchDecide_thenReturns200() throws Exception {
        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "ok");
        when(alertService.decide(eq("a1"), eq("tenant-1"), any(DecisionRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/alerts/a1/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(sampleResponse)));
    }

    @Test
    void givenAlertAlreadyDecided_whenPatchDecide_thenReturns409() throws Exception {
        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "ok");
        when(alertService.decide(eq("a1"), eq("tenant-1"), any(DecisionRequest.class)))
                .thenThrow(new AlertAlreadyDecidedException("already"));

        mockMvc.perform(patch("/api/v1/alerts/a1/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isConflict());
    }

    @Test
    void givenAlertNotFound_whenPatchDecide_thenReturns404() throws Exception {
        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "ok");
        when(alertService.decide(eq("a1"), eq("tenant-1"), any(DecisionRequest.class)))
                .thenThrow(new AlertNotFoundException("a1"));

        mockMvc.perform(patch("/api/v1/alerts/a1/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenValidEscalate_whenPatchEscalate_thenReturns200() throws Exception {
        when(alertService.escalate("a1", "tenant-1")).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/alerts/a1/escalate").header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(sampleResponse)));
    }

    @Test
    void givenInvalidTransition_whenPatchEscalate_thenReturns400() throws Exception {
        when(alertService.escalate("a1", "tenant-1")).thenThrow(new InvalidTransitionException("bad"));

        mockMvc.perform(patch("/api/v1/alerts/a1/escalate").header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAlertNotFound_whenPatchEscalate_thenReturns404() throws Exception {
        when(alertService.escalate("a1", "tenant-1")).thenThrow(new AlertNotFoundException("a1"));

        mockMvc.perform(patch("/api/v1/alerts/a1/escalate").header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isNotFound());
    }
}