package com.fincom.alerts.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.api.dto.DecisionRequest;
import com.fincom.alerts.domain.AlertStatus;
import com.fincom.alerts.service.AlertService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    public ResponseEntity<AlertResponse> create(@Valid @RequestBody CreateAlertRequest request,
    		@TenantId String tenantId) {
        AlertResponse created = alertService.create(request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<AlertResponse> list(@TenantId String tenantId,
                                    @RequestParam(required = false) AlertStatus status,
                                    @RequestParam(required = false) @Min(0) @Max(100) Integer minMatchScore) {
        return alertService.list(tenantId, status, minMatchScore);
    }

    @PatchMapping("/{id}/decision")
    public AlertResponse decide(@PathVariable String id, @Valid @RequestBody DecisionRequest request, 
    		@TenantId String tenantId) {
        return alertService.decide(id, tenantId, request);
    }

    @PatchMapping("/{id}/escalate")
    public AlertResponse escalate(@PathVariable String id, @TenantId String tenantId) {
        return alertService.escalate(id, tenantId);
    }
}
