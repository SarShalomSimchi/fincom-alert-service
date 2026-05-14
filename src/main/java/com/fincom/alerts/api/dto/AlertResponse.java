package com.fincom.alerts.api.dto;

import com.fincom.alerts.domain.AlertStatus;
import java.time.Instant;

public record AlertResponse(
        String id,
        String transactionId,
        String matchedEntityName,
        int matchScore,
        AlertStatus status,
        String assignedTo,
        String tenantId,
        Instant createdAt,
        Instant updatedAt,
        String decisionNote
) {
}
