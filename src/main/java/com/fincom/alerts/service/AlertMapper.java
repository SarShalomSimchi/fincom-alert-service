package com.fincom.alerts.service;

import org.springframework.stereotype.Component;

import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.domain.Alert;
import com.fincom.alerts.domain.AlertStatus;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        if (alert == null) {
            return null;
        }
        
        return new AlertResponse(
                alert.getId(),
                alert.getTransactionId(),
                alert.getMatchedEntityName(),
                alert.getMatchScore(),
                alert.getStatus(),
                alert.getAssignedTo(),
                alert.getTenantId(),
                alert.getCreatedAt(),
                alert.getUpdatedAt(),
                alert.getDecisionNote()
        );
    }

    public Alert toEntity(CreateAlertRequest request, String tenantId) {
        if (request == null) {
            return null;
        }
        
        return Alert.builder()
                .transactionId(request.transactionId())
                .matchedEntityName(request.matchedEntityName())
                .matchScore(request.matchScore())
                .assignedTo(request.assignedTo())
                .tenantId(tenantId)
                .status(AlertStatus.OPEN)
                .build();
    }
}
