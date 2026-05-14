# Sanctions Alert Service

## How to Run

Prerequisites: Java 21, Maven

Command to run:

mvn spring-boot:run

Command to run tests:

mvn test

Base URL: http://localhost:8080/api/v1

H2 console available at: http://localhost:8080/h2-console

JDBC URL: jdbc:h2:mem:alertsdb

Username: sa

Password: (none)

---

## API Endpoints

| Method | Path | Description | Required Header |
|--------|------|-------------|-----------------|
| POST | /api/v1/alerts | Create a new alert | X-Tenant-ID |
| GET  | /api/v1/alerts | List alerts with optional filters (status, minMatchScore) | X-Tenant-ID |
| PATCH | /api/v1/alerts/{id}/decision | Decide an alert (CLEARED or CONFIRMED_HIT) with a decision note | X-Tenant-ID |
| PATCH | /api/v1/alerts/{id}/escalate | Escalate an OPEN alert to ESCALATED | X-Tenant-ID |

Examples (curl)

- Create alert

curl -i -X POST http://localhost:8080/api/v1/alerts \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-1" \
  -d '{"transactionId":"tx-123","matchedEntityName":"John Doe","matchScore":85,"assignedTo":"analyst-1"}'

- List alerts

curl -i -X GET "http://localhost:8080/api/v1/alerts?status=OPEN&minMatchScore=80" \
  -H "X-Tenant-ID: tenant-1"

- Decide an alert

curl -i -X PATCH http://localhost:8080/api/v1/alerts/{id}/decision \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-1" \
  -d '{"decision":"CLEARED","decisionNote":"Investigated and cleared"}'

- Escalate an alert

curl -i -X PATCH http://localhost:8080/api/v1/alerts/{id}/escalate \
  -H "X-Tenant-ID: tenant-1"

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
- Threat model assumption: this service sits behind an API gateway that authenticates callers; the gateway is responsible for injecting/validating X-Tenant-ID so the service trusts the header after a presence check.

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
- Add Spring Security with JWT authentication
- Validate tenant identity against an identity provider rather than trusting the header directly
- Add rate limiting per tenant

### Observability
- Add MDC with tenantId and requestId for distributed tracing across log lines
- Integrate OpenTelemetry for traces and metrics
- Configure async logging via Logback AsyncAppender or migrate to Log4J2 for high-throughput scenarios

### API
- Add pagination to the list endpoint (page, size query params)
- Add API versioning strategy
- Add Swagger/OpenAPI documentation via springdoc-openapi

### Resilience
- Add retry logic on event publishing failures
- Add a dead-letter mechanism for failed events
- Add circuit breaker if the service calls external APIs

---

If you want, I can also update the project's README.txt notes into this README.md or add an OpenAPI description file for the endpoints.