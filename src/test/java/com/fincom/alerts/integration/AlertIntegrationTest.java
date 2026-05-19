package com.fincom.alerts.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fincom.alerts.api.ApiHeaders;
import com.fincom.alerts.api.dto.AlertResponse;
import com.fincom.alerts.api.dto.CreateAlertRequest;
import com.fincom.alerts.api.dto.DecisionRequest;
import com.fincom.alerts.domain.AlertStatus;
import com.fincom.alerts.repository.AlertRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AlertIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AlertRepository repository;

    private static final String BASE = "/api/v1/alerts";

    @BeforeEach
    void beforeEach() {
        repository.deleteAll();
    }

    private AlertResponse createAlert(String tenantId, String transactionId, String matchedEntityName, int matchScore) {
        CreateAlertRequest req = new CreateAlertRequest(transactionId, matchedEntityName, matchScore, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, tenantId);
        HttpEntity<CreateAlertRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<AlertResponse> resp = restTemplate.postForEntity(BASE, entity, AlertResponse.class);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        return resp.getBody();
    }

    private AlertResponse createAlert(String tenantId) {
        String tx = "tx-" + UUID.randomUUID();
        return createAlert(tenantId, tx, "entity", 50);
    }

    @Test
    void givenValidRequest_whenCreateAlert_thenReturns201WithIdAndTimestampsAndStatusOpen() {
        AlertResponse resp = createAlert("tenant-1", "tx-1", "ent-1", 60);
        assertNotNull(resp);
        assertNotNull(resp.id());
        assertNotNull(resp.createdAt());
        assertNotNull(resp.updatedAt());
        assertEquals(AlertStatus.OPEN, resp.status());
    }

    @Test
    void givenNoTenantHeader_whenCreateAlert_thenReturns400() {
        CreateAlertRequest req = new CreateAlertRequest("tx-2", "ent-2", 30, null);
        HttpEntity<CreateAlertRequest> entity = new HttpEntity<>(req);
        ResponseEntity<String> resp = restTemplate.postForEntity(BASE, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void givenInvalidMatchScore_whenCreateAlert_thenReturns400() {
        CreateAlertRequest req = new CreateAlertRequest("tx-3", "ent-3", 200, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");
        HttpEntity<CreateAlertRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(BASE, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void givenBlankTransactionId_whenCreateAlert_thenReturns400() {
        CreateAlertRequest req = new CreateAlertRequest("", "ent-4", 30, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");
        HttpEntity<CreateAlertRequest> entity = new HttpEntity<>(req, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(BASE, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void givenMultipleTenants_whenList_thenReturnsOnlyTenantAlerts() {
        createAlert("tenant-1", "tx-a", "e-a", 10);
        createAlert("tenant-2", "tx-b", "e-b", 20);

        HttpHeaders headers = new HttpHeaders();
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");
        ResponseEntity<AlertResponse[]> resp = restTemplate.exchange(BASE, HttpMethod.GET, new HttpEntity<>(headers), AlertResponse[].class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<AlertResponse> list = Arrays.asList(resp.getBody());
        assertEquals(1, list.size());
        assertEquals("tenant-1", list.get(0).tenantId());
    }

    @Test
    void givenAlertsWithDifferentStatus_whenListWithStatusOpen_thenReturnsOnlyOpen() {
        AlertResponse a1 = createAlert("tenant-1", "tx-s1", "e1", 10);
        // escalate a1 to ESCALATED
        HttpHeaders headers = new HttpHeaders();
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");
        ResponseEntity<AlertResponse> escResp = restTemplate.exchange(BASE + "/" + a1.id() + "/escalate", HttpMethod.PATCH, new HttpEntity<>(headers), AlertResponse.class);
        assertEquals(HttpStatus.OK, escResp.getStatusCode());

        // create open alert
        createAlert("tenant-1", "tx-s2", "e2", 20);

        ResponseEntity<AlertResponse[]> listResp = restTemplate.exchange(BASE + "?status=OPEN", HttpMethod.GET, new HttpEntity<>(headers), AlertResponse[].class);
        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        List<AlertResponse> list = Arrays.asList(listResp.getBody());
        assertTrue(list.stream().allMatch(a -> a.status() == AlertStatus.OPEN));
    }

    @Test
    void givenAlertsWithDifferentScores_whenListWithMinMatchScore_thenFiltersCorrectly() {
        createAlert("tenant-1", "tx-m1", "e1", 70);
        createAlert("tenant-1", "tx-m2", "e2", 85);

        HttpHeaders headers = new HttpHeaders();
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");
        ResponseEntity<AlertResponse[]> resp = restTemplate.exchange(BASE + "?minMatchScore=80", HttpMethod.GET, new HttpEntity<>(headers), AlertResponse[].class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<AlertResponse> list = Arrays.asList(resp.getBody());
        assertTrue(list.stream().allMatch(a -> a.matchScore() >= 80));
    }

    @Test
    void givenFilters_whenListWithStatusAndMinMatchScore_thenFiltersByBoth() {
        createAlert("tenant-1", "tx-f1", "e1", 90); // open 90
        AlertResponse a2 = createAlert("tenant-1", "tx-f2", "e2", 85); // open 85
        // escalate a2 so status not OPEN
        HttpHeaders headers = new HttpHeaders();
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");
        restTemplate.exchange(BASE + "/" + a2.id() + "/escalate", HttpMethod.PATCH, new HttpEntity<>(headers), AlertResponse.class);

        ResponseEntity<AlertResponse[]> resp = restTemplate.exchange(BASE + "?status=OPEN&minMatchScore=80", HttpMethod.GET, new HttpEntity<>(headers), AlertResponse[].class);
        List<AlertResponse> list = Arrays.asList(resp.getBody());
        assertTrue(list.stream().allMatch(a -> a.status() == AlertStatus.OPEN && a.matchScore() >= 80));
    }

    @Test
    void givenOpenAlert_whenEscalate_thenTransitionsToEscalatedAndReturns200() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");

        ResponseEntity<AlertResponse> resp = restTemplate.exchange(BASE + "/" + created.id() + "/escalate", HttpMethod.PATCH, new HttpEntity<>(headers), AlertResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(AlertStatus.ESCALATED, resp.getBody().status());
    }

    @Test
    void givenEscalatedAlert_whenEscalateAgain_thenReturnsUnprocessableEntity() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");
        // first escalate
        restTemplate.exchange(BASE + "/" + created.id() + "/escalate", HttpMethod.PATCH, new HttpEntity<>(headers), AlertResponse.class);
        // second escalate
        ResponseEntity<String> resp2 = restTemplate.exchange(BASE + "/" + created.id() + "/escalate", HttpMethod.PATCH, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());
    }

    @Test
    void givenAlertFromDifferentTenant_whenEscalate_thenReturnsNotFound() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.add(ApiHeaders.TENANT_ID, "tenant-2");
        ResponseEntity<String> resp = restTemplate.exchange(BASE + "/" + created.id() + "/escalate", HttpMethod.PATCH, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void givenValidDecisionCleared_whenDecide_thenTransitionsAndReturns200() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");

        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "cleared-note");
        ResponseEntity<AlertResponse> resp = restTemplate.exchange(BASE + "/" + created.id() + "/decision", HttpMethod.PATCH, new HttpEntity<>(req, headers), AlertResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(AlertStatus.CLEARED, resp.getBody().status());
    }

    @Test
    void givenValidDecisionConfirmedHit_whenDecide_thenTransitionsAndReturns200() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");

        DecisionRequest req = new DecisionRequest(AlertStatus.CONFIRMED_HIT, "hit-note");
        ResponseEntity<AlertResponse> resp = restTemplate.exchange(BASE + "/" + created.id() + "/decision", HttpMethod.PATCH, new HttpEntity<>(req, headers), AlertResponse.class);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(AlertStatus.CONFIRMED_HIT, resp.getBody().status());
    }

    @Test
    void givenDecidedAlert_whenDecideAgain_thenReturnsConflict() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");

        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "note1");
        restTemplate.exchange(BASE + "/" + created.id() + "/decision", HttpMethod.PATCH, new HttpEntity<>(req, headers), AlertResponse.class);
        // second call
        ResponseEntity<String> resp2 = restTemplate.exchange(BASE + "/" + created.id() + "/decision", HttpMethod.PATCH, new HttpEntity<>(req, headers), String.class);
        assertEquals(HttpStatus.CONFLICT, resp2.getStatusCode());
    }

    @Test
    void givenAlertFromDifferentTenant_whenDecide_thenReturnsNotFound() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, "tenant-2");

        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "note");
        ResponseEntity<String> resp = restTemplate.exchange(BASE + "/" + created.id() + "/decision", HttpMethod.PATCH, new HttpEntity<>(req, headers), String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void givenBlankDecisionNote_whenDecide_thenReturnsBadRequest() {
        AlertResponse created = createAlert("tenant-1");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(ApiHeaders.TENANT_ID, "tenant-1");

        DecisionRequest req = new DecisionRequest(AlertStatus.CLEARED, "");
        ResponseEntity<String> resp = restTemplate.exchange(BASE + "/" + created.id() + "/decision", HttpMethod.PATCH, new HttpEntity<>(req, headers), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void givenTwoTenants_whenCreateAlerts_thenListingIsIsolated() {
        createAlert("tenant-1", "tx-t1", "e1", 10);
        createAlert("tenant-2", "tx-t2", "e2", 20);

        HttpHeaders h1 = new HttpHeaders();
        h1.add(ApiHeaders.TENANT_ID, "tenant-1");
        ResponseEntity<AlertResponse[]> r1 = restTemplate.exchange(BASE, HttpMethod.GET, new HttpEntity<>(h1), AlertResponse[].class);
        assertEquals(1, Arrays.asList(r1.getBody()).size());

        HttpHeaders h2 = new HttpHeaders();
        h2.add(ApiHeaders.TENANT_ID, "tenant-2");
        ResponseEntity<AlertResponse[]> r2 = restTemplate.exchange(BASE, HttpMethod.GET, new HttpEntity<>(h2), AlertResponse[].class);
        assertEquals(1, Arrays.asList(r2.getBody()).size());
    }
}
