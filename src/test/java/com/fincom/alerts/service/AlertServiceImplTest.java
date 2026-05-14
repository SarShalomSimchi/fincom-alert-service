package com.fincom.alerts.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.api.dto.DecisionRequest;
import com.fincom.alerts.domain.Alert;
import com.fincom.alerts.domain.AlertStatus;
import com.fincom.alerts.domain.AlertValidator;
import com.fincom.alerts.event.EventPublisher;
import com.fincom.alerts.exception.AlertAlreadyDecidedException;
import com.fincom.alerts.exception.AlertNotFoundException;
import com.fincom.alerts.exception.InvalidTransitionException;
import com.fincom.alerts.repository.AlertRepository;

@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    AlertRepository repository;

    @Mock
    AlertValidator validator;

    @Mock
    AlertMapper mapper;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    AlertServiceImpl service;

    private Alert alertEntity;
    private AlertResponse alertResponse;

    @BeforeEach
    void setup() {
        alertEntity = Alert.builder()
                .id("a1")
                .transactionId("tx1")
                .matchedEntityName("ent")
                .matchScore(50)
                .status(AlertStatus.OPEN)
                .tenantId("tenant-1")
                .build();

        alertResponse = new AlertResponse("a1", "tx1", "ent", 50, AlertStatus.OPEN, null, "tenant-1", null, null, null);
    }

    @Test
    void givenValidCreateRequest_whenCreate_thenSavesAndReturnsResponse() {
        CreateAlertRequest req = new CreateAlertRequest("tx1", "ent", 50, null);
        when(mapper.toEntity(req, "tenant-1")).thenReturn(alertEntity);
        when(repository.save(alertEntity)).thenReturn(alertEntity);
        when(mapper.toResponse(alertEntity)).thenReturn(alertResponse);

        AlertResponse res = service.create(req, "tenant-1");

        verify(repository).save(alertEntity);
        assertEquals("a1",res.id());
    }

    @Test
    void givenExistingAlerts_whenList_thenReturnsMappedList() {
        when(repository.findByFilter("tenant-1", null, null)).thenReturn(List.of(alertEntity));
        when(mapper.toResponse(alertEntity)).thenReturn(alertResponse);

        List<AlertResponse> results = service.list("tenant-1", null, null);

        verify(repository).findByFilter("tenant-1", null, null);
        assertEquals(1, results.size());
        assertEquals("a1", results.get(0).id());
    }

    @Test
    void givenOpenAlert_whenDecideWithCleared_thenUpdatesSavesAndPublishes() {
        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "ok");
        when(repository.findByIdAndTenantId("a1", "tenant-1")).thenReturn(Optional.of(alertEntity));
        doNothing().when(validator).validateDecide(alertEntity);

        // mapper not involved here for inputs/outputs; repository.save returns updated entity
        when(repository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Alert.class))).thenReturn(alertResponse);

        service.decide("a1", "tenant-1", req);

        verify(repository).findByIdAndTenantId("a1", "tenant-1");
        verify(repository).save(any(Alert.class));
        verify(eventPublisher).publish(any(com.fincom.alerts.event.AlertEvent.class));
    }

    @Test
    void givenMissingAlert_whenDecide_thenThrowsNotFound() {
        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "ok");
        when(repository.findByIdAndTenantId("a1", "tenant-1")).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, () -> service.decide("a1", "tenant-1", req));
    }

    @Test
    void givenAlert_whenDecideWithInvalidDecision_thenThrowsInvalidTransition() {
        DecisionRequest req = new DecisionRequest(AlertStatus.OPEN, "note");

        assertThrows(InvalidTransitionException.class, () -> service.decide("a1", "tenant-1", req));
    }

    @Test
    void givenAlreadyDecidedAlert_whenDecide_thenPropagatesAlreadyDecided() {
        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "note");
        when(repository.findByIdAndTenantId("a1", "tenant-1")).thenReturn(Optional.of(alertEntity));
        doThrow(new AlertAlreadyDecidedException("already")).when(validator).validateDecide(alertEntity);

        assertThrows(AlertAlreadyDecidedException.class, () -> service.decide("a1", "tenant-1", req));
    }

    @Test
    void givenOpenAlert_whenEscalate_thenUpdatesSavesAndPublishes() {
        when(repository.findByIdAndTenantId("a1", "tenant-1")).thenReturn(Optional.of(alertEntity));
        doNothing().when(validator).validateEscalate(alertEntity);
        when(repository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Alert.class))).thenReturn(alertResponse);

        service.escalate("a1", "tenant-1");

        verify(repository).findByIdAndTenantId("a1", "tenant-1");
        verify(repository).save(any(Alert.class));
        verify(eventPublisher).publish(any(com.fincom.alerts.event.AlertEvent.class));
    }

    @Test
    void givenMissingAlert_whenEscalate_thenThrowsNotFound() {
        when(repository.findByIdAndTenantId("a1", "tenant-1")).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, () -> service.escalate("a1", "tenant-1"));
    }

    @Test
    void givenInvalidTransition_whenEscalate_thenPropagatesInvalidTransition() {
        when(repository.findByIdAndTenantId("a1", "tenant-1")).thenReturn(Optional.of(alertEntity));
        doThrow(new InvalidTransitionException("bad")).when(validator).validateEscalate(alertEntity);

        assertThrows(InvalidTransitionException.class, () -> service.escalate("a1", "tenant-1"));
    }
}