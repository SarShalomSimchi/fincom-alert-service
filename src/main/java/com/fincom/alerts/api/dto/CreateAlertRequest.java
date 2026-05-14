package com.fincom.alerts.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAlertRequest(
        @NotBlank String transactionId,
        @NotBlank String matchedEntityName,
        @NotNull @Min(0) @Max(100) Integer matchScore,
        String assignedTo
) {
}
