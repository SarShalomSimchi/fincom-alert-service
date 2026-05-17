# Sanctions Alert Service

## How to Run

Prerequisites: Java 21, Maven

Command to run:

mvn spring-boot:run

Command to run tests:

mvn test

Base URL: http://localhost:8080/api/v1

## Local Database

The application uses an in-memory H2 database for local development and automated tests.

```text
JDBC URL: jdbc:h2:mem:alertsdb
Username: sa
Password: <empty>
```

The H2 web console is not enabled by default.

If the H2 console is needed for local debugging, add the required Spring Boot H2 console dependency and enable it only in local configuration.

For Spring Boot 4, enabling the H2 web console may require the `spring-boot-h2console` dependency in addition to the console configuration.

Example local configuration:

```yaml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

Do not rely on the H2 in-memory database for production. It is intended only for local development and automated tests.  
---

## API Endpoints

| Method | Path | Description | Required Header |
|--------|------|-------------|-----------------|
| POST | /api/v1/alerts | Create a new alert | X-Tenant-ID |
| GET  | /api/v1/alerts | List alerts with optional filters (status, minMatchScore) | X-Tenant-ID |
| PATCH | /api/v1/alerts/{id}/decision | Decide an alert (CLEARED or CONFIRMED_HIT) with a decision note | X-Tenant-ID |
| PATCH | /api/v1/alerts/{id}/escalate | Escalate an OPEN alert to ESCALATED | X-Tenant-ID |


### API Versioning

The service uses URL-based API versioning.

Current endpoints are exposed under `/api/v1`.

Backward-compatible changes may remain under the existing version. Breaking API contract changes should be introduced under a new version path, such as `/api/v2`, while keeping `/api/v1` available during a deprecation period when possible.


## API Usage Examples

By default, the application runs on:

```text
http://localhost:8080
```

If port `8080` is already in use, run the application on another port, for example:

```bash
--server.port=8081
```

Then replace `8080` with `8081` in the examples below.

---

## Create Alert

### PowerShell

```powershell
curl.exe -i -X POST "http://localhost:8080/api/v1/alerts" -H "Content-Type: application/json" -H "X-Tenant-ID: tenant-1" -d '{"transactionId":"tx-123","matchedEntityName":"John Doe","matchScore":85,"assignedTo":"analyst-1"}'
```

### Git Bash / Linux / macOS

```bash
curl -i -X POST "http://localhost:8080/api/v1/alerts" \
-H "Content-Type: application/json" \
-H "X-Tenant-ID: tenant-1" \
-d '{"transactionId":"tx-123","matchedEntityName":"John Doe","matchScore":85,"assignedTo":"analyst-1"}'
```

### Expected Response

```json
{
  "id": "generated-alert-id",
  "transactionId": "tx-123",
  "matchedEntityName": "John Doe",
  "matchScore": 85,
  "status": "OPEN",
  "assignedTo": "analyst-1",
  "tenantId": "tenant-1",
  "createdAt": "2026-05-14T20:06:34.639616100Z",
  "updatedAt": "2026-05-14T20:06:34.639616100Z",
  "decisionNote": null
}
```

---

## List Alerts

### PowerShell

```powershell
curl.exe -i -X GET "http://localhost:8080/api/v1/alerts?status=OPEN&minMatchScore=80" -H "X-Tenant-ID: tenant-1"
```

### Git Bash / Linux / macOS

```bash
curl -i -X GET "http://localhost:8080/api/v1/alerts?status=OPEN&minMatchScore=80" \
-H "X-Tenant-ID: tenant-1"
```

---

## Escalate Alert

Replace `{id}` with the alert ID returned from the create alert response.

### PowerShell

```powershell
curl.exe -i -X PATCH "http://localhost:8080/api/v1/alerts/{id}/escalate" -H "X-Tenant-ID: tenant-1"
```

### Git Bash / Linux / macOS

```bash
curl -i -X PATCH "http://localhost:8080/api/v1/alerts/{id}/escalate" \
-H "X-Tenant-ID: tenant-1"
```

---

## Decide Alert

Replace `{id}` with the alert ID returned from the create alert response.

### PowerShell

```powershell
curl.exe -i -X PATCH "http://localhost:8080/api/v1/alerts/{id}/decision" -H "Content-Type: application/json" -H "X-Tenant-ID: tenant-1" -d '{"decision":"CLEARED","decisionNote":"Investigated and cleared"}'
```

### Git Bash / Linux / macOS

```bash
curl -i -X PATCH "http://localhost:8080/api/v1/alerts/{id}/decision" \
-H "Content-Type: application/json" \
-H "X-Tenant-ID: tenant-1" \
-d '{"decision":"CLEARED","decisionNote":"Investigated and cleared"}'
```

---

## Notes

- `X-Tenant-ID` is required for all API requests.
- `matchScore` must be between `0` and `100`.
- New alerts are created with status `OPEN`.
- Alerts can be escalated from `OPEN` to `ESCALATED`.
- Decision actions can mark alerts as `CLEARED` or `CONFIRMED_HIT`, depending on the supported state transition rules.
- On Windows PowerShell, use `curl.exe` instead of `curl`.
- In PowerShell, prefer one-line commands to avoid line-continuation issues.

---

## Design Decisions

### API Design

- POST /alerts for create — semantically correct for resource creation, returns 201 Created with the created resource.
- GET /alerts for list — supports optional query parameters (status, minMatchScore) to filter results.
- PATCH /alerts/{id}/decision for decide — a focused partial update for a specific business operation (decision) rather than replacing the whole resource.
- PATCH /alerts/{id}/escalate for escalate — an operation-style endpoint communicating intent clearly.

### Tenant Isolation

- Tenant identity is carried via the X-Tenant-ID request header.
- TenantArgumentResolver extracts and validates the header before controller methods are invoked.
- All repository queries include tenantId as a mandatory filter; single-resource fetches use findByIdAndTenantId to prevent cross-tenant access.
- Requests missing or containing a blank X-Tenant-ID are rejected with 400 Bad Request.
- Threat model assumption: in production, this service is expected to run behind an API gateway that authenticates callers and injects or validates X-Tenant-ID. The service performs only a defensive presence check for the header and then trusts the tenant value provided by the gateway. Requests missing X-Tenant-ID are rejected to protect local runs, tests, and misconfigured routing paths, but tenant authorization is intentionally delegated to the gateway.

Why X-Tenant-ID is carried in the header — alternatives considered:

Three common patterns exist for carrying tenant identity in a multi-tenant API:

Option 1 — Request Header (X-Tenant-ID: tenant-123) ✅ chosen

Pros:
- Clean separation between identity (header) and resource (body/path)
- Naturally enforced in one place via TenantArgumentResolver — no risk of forgetting it in a request body
- Easy for API gateways to inject or validate centrally before the request reaches the service
- Does not pollute the request body or URL with infrastructure concerns

Cons:
- Header can be spoofed if the service is exposed directly without a gateway — mitigated by assuming the service sits behind an authenticated gateway
- Less visible in browser-based tools like Swagger UI where headers can be easy to overlook

Option 2 — URL Path (/api/v1/tenants/{tenantId}/alerts)

Pros:
- Tenant is explicit and visible in every URL
- Works naturally with REST resource hierarchy
- Easy to test in a browser or Swagger UI without setting headers

Cons:
- Tenant leaks into every route definition — all controllers must include {tenantId} in every path
- Increases risk of bugs where tenantId in the path doesn't match the authenticated caller's tenant
- URLs become verbose and harder to read

Option 3 — JWT Claim (token in Authorization: Bearer <token>)

Pros:
- Most secure option — tenant identity is cryptographically signed, cannot be spoofed
- Standard in production systems with Spring Security
- No need to trust an external header — the token itself is the proof of identity
- Carries additional claims (roles, permissions) alongside tenant identity

Cons:
- Requires Spring Security and an identity provider (Keycloak, Auth0, etc.)
- Significantly more infrastructure for a small service
- Over-engineering for this assignment scope

Why header over path:

The path option pollutes every route and creates a mismatch risk between the path variable and the authenticated caller. The header cleanly separates the concern of "who is calling" from "what resource are they accessing" — which maps well to how API gateways work in practice (the gateway authenticates, strips the token, and injects a trusted X-Tenant-ID header downstream).

Why header over JWT for this assignment:

JWT is the production choice and would be the first thing added before going live. For this scope, the header approach demonstrates the same isolation guarantees with a simpler implementation — the TenantArgumentResolver and repository-level tenantId filtering would remain unchanged when swapping to JWT; only the resolver's extraction logic would change from reading a header to reading a token claim.

### State Machine

- OPEN → ESCALATED (escalate endpoint)
- OPEN → CLEARED / CONFIRMED_HIT (decision endpoint)
- ESCALATED → CLEARED / CONFIRMED_HIT (decision endpoint)
- Decisions are write-once — attempting to re-decide returns 409 Conflict

Note: The spec does not explicitly state that ESCALATED alerts can be decided. We assume ESCALATED → CLEARED/CONFIRMED_HIT is valid since escalation implies pending senior review which must eventually reach a decision. This is a one-line change if the intent is otherwise.

### Event Publishing

- Events are published on escalation and decision — not on creation.
- EventPublisher is an interface; LoggingEventPublisher is the current implementation and serializes events as JSON to stdout via SLF4J.
- Swapping to Kafka, SQS, or RabbitMQ requires only a new implementation of EventPublisher — no changes to the service layer.

### Repository Pattern

- AlertRepository extends JpaRepository and provides tenant-aware queries (findByIdAndTenantId, findByFilter).
- The project uses H2 for tests and runtime by default. Swapping to PostgreSQL requires only a driver and datasource configuration change.

### Compromises & Deviations

- No authentication implemented in this sample — the service assumes it sits behind an authenticated API gateway.
- LoggingEventPublisher used instead of a real message broker — the EventPublisher interface makes swapping straightforward.
- No pagination on the list endpoint — acceptable for this scope.

---

## What I Would Add for Production

### Infrastructure
- Replace H2 with PostgreSQL and add Flyway or Liquibase for schema migrations
- Replace LoggingEventPublisher with a Kafka or SQS publisher implementation

### Security

Production hardening should be implemented at the API gateway / ingress layer where possible.

- Authenticate callers using JWT.
- Validate tenant identity against an identity provider or authorization service.
- Inject or validate `X-Tenant-ID` only after authentication and tenant authorization succeed.
- Configure per-tenant rate limiting at the gateway / ingress layer using the authenticated tenant identity or validated `X-Tenant-ID` as the rate-limit key.
- Keep the service-side `X-Tenant-ID` presence check as a defensive guard.

The service currently trusts `X-Tenant-ID` after a presence check, based on the assumption that upstream infrastructure has already authenticated the caller and validated the tenant.

### Observability
- Add MDC-based logging context with `tenantId` and `requestId`, so log lines from the same request can be correlated across the service and downstream calls.
- Integrate OpenTelemetry for distributed traces and metrics, so production deployments can monitor request latency, error rates, downstream dependencies, and tenant-level operational behavior.
- For high-throughput production deployments, configure asynchronous logging using Logback `AsyncAppender` or consider Log4J2 async logging to reduce request-thread blocking caused by log I/O.

### API
- Add pagination to the list endpoint (page, size query params)
- Formalize and maintain the API versioning policy as the API evolves
- - Add Swagger/OpenAPI documentation using `springdoc-openapi`, so API endpoints, request/response schemas, headers, query parameters, and status codes are documented and testable through Swagger UI.

### Resilience
- Add bounded retry logic for transient event publishing failures, with backoff to avoid overwhelming the broker.
- Add a dead-letter mechanism for permanently failed events, such as a Kafka dead-letter topic, SQS dead-letter queue, or database-backed failed-event table, so publishing failures can be investigated and replayed.
- Add circuit breakers, timeouts, and fallback handling if the service later calls external APIs or downstream services.

---