package com.fincom.alerts.service;

import java.util.List;

import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.api.dto.DecisionRequest;
import com.fincom.alerts.domain.AlertStatus;

public interface AlertService {

    AlertResponse create(CreateAlertRequest request, String tenantId);

    List<AlertResponse> list(String tenantId, AlertStatus status, Integer minMatchScore);

    AlertResponse decide(String alertId, String tenantId, DecisionRequest request);

    AlertResponse escalate(String alertId, String tenantId);
}
