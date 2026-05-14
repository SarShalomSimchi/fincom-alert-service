package com.fincom.alerts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.domain.Alert;
import com.fincom.alerts.domain.AlertStatus;

class AlertMapperTest {

    private final AlertMapper mapper = new AlertMapper();

    @Test
    void givenAlert_whenToResponse_thenMapsAllFields() {
        Instant now = Instant.now();
        Alert alert = Alert.builder()
                .id("id-1")
                .transactionId("tx-1")
                .matchedEntityName("ent")
                .matchScore(75)
                .status(AlertStatus.OPEN)
                .assignedTo("user-1")
                .tenantId("tenant-1")
                .createdAt(now)
                .updatedAt(now)
                .decisionNote("note")
                .build();

        AlertResponse resp = mapper.toResponse(alert);

        assertEquals("id-1", resp.id());
        assertEquals("tx-1", resp.transactionId());
        assertEquals("ent", resp.matchedEntityName());
        assertEquals(75, resp.matchScore());
        assertEquals(AlertStatus.OPEN, resp.status());
        assertEquals("user-1", resp.assignedTo());
        assertEquals("tenant-1", resp.tenantId());
        assertEquals(now, resp.createdAt());
        assertEquals(now, resp.updatedAt());
        assertEquals("note", resp.decisionNote());
    }

    @Test
    void givenCreateRequestAndTenant_whenToEntity_thenMapsFieldsAndSetsStatusOpen_andLeavesTimestampsNull() {
        CreateAlertRequest req = new CreateAlertRequest("tx-2", "entity-2", 50, "assignee");
        Alert entity = mapper.toEntity(req, "tenant-2");

        assertNull(entity.getId());
        assertEquals("tx-2", entity.getTransactionId());
        assertEquals("entity-2", entity.getMatchedEntityName());
        assertEquals(50, entity.getMatchScore());
        assertEquals("assignee", entity.getAssignedTo());
        assertEquals("tenant-2", entity.getTenantId());
        assertEquals(AlertStatus.OPEN, entity.getStatus());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }
}