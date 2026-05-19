package com.fincom.alerts.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.api.dto.DecisionRequest;
import com.fincom.alerts.domain.Alert;
import com.fincom.alerts.domain.AlertStatus;
import com.fincom.alerts.domain.AlertValidator;
import com.fincom.alerts.event.AlertDecidedEvent;
import com.fincom.alerts.event.AlertEscalatedEvent;
import com.fincom.alerts.event.EventPublisher;
import com.fincom.alerts.exception.AlertNotFoundException;
import com.fincom.alerts.exception.InvalidTransitionException;
import com.fincom.alerts.repository.AlertRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository repository;
    private final AlertValidator validator;
    private final AlertMapper mapper;
    private final EventPublisher eventPublisher;


    @Override
    @Transactional
    public AlertResponse create(CreateAlertRequest request, String tenantId) {
        Alert entity = mapper.toEntity(request, tenantId);
        Alert saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> list(String tenantId, AlertStatus status, Integer minMatchScore) {
        List<Alert> results = repository.findByFilter(tenantId, status, minMatchScore);
        return results.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public AlertResponse decide(String alertId, String tenantId, DecisionRequest request) {
        AlertStatus decision = request.decision();

        validator.validateDecisionStatus(decision);

        Alert alert = repository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        validator.validateDecide(alert);

        alert.setStatus(decision);
        alert.setDecisionNote(request.decisionNote());

        Alert saved = repository.save(alert);

        eventPublisher.publish(AlertDecidedEvent.of(saved));

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AlertResponse escalate(String alertId, String tenantId) {
        Alert alert = repository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        validator.validateEscalate(alert);

        alert.setStatus(AlertStatus.ESCALATED);

        Alert saved = repository.save(alert);

        eventPublisher.publish(AlertEscalatedEvent.of(saved));

        return mapper.toResponse(saved);
    }
}