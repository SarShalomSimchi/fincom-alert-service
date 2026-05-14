package com.fincom.alerts.api.dto;

import com.fincom.alerts.domain.AlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
        @NotNull AlertStatus decision,
        @NotBlank String decisionNote
) {
}
