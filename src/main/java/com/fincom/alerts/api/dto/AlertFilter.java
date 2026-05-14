package com.fincom.alerts.api.dto;

import com.fincom.alerts.domain.AlertStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AlertFilter(
        AlertStatus status,
        @Min(0) @Max(100) Integer minMatchScore
) {
}
