# Fincom — Backend Home Assignment

## Sanctions Alert Service

### Context

You're joining a financial compliance platform that screens payment transactions against sanctions lists. When a
transaction matches a sanctioned entity, an **alert** is created for a compliance officer to review. Your task is to
build a small **Alert Management Service**.

---

### Requirements

Build a REST API service (in **Go or Kotlin/Java preferred**, but any language **excluding JavaScript** is acceptable)
that manages screening alerts with the following:

#### Domain Model

- **Alert**: Represents a potential sanctions match on a transaction. Types are your choice; the semantics below are the contract.
    - `id` — server-generated unique identifier
    - `transactionId` — identifier of the screened transaction (opaque to this service)
    - `matchedEntityName` — the sanctioned entity that was matched
    - `matchScore` — match confidence in the range `0–100`, inclusive
    - `status` — one of `OPEN`, `ESCALATED`, `CLEARED`, `CONFIRMED_HIT`
    - `assignedTo` — analyst identifier; optional
    - `tenantId` — tenant the alert belongs to (see *Constraints & Rules* for how it's carried through requests)
    - `createdAt`, `updatedAt` — creation and last-modification timestamps
    - `decisionNote` — analyst's reasoning; required when submitting a decision, otherwise absent

#### Operations

The service must support the operations below. **You decide the HTTP verbs, paths, and request/response shapes** —
document and defend the choices in your README.

1. **Create alert** — accept a new alert from an upstream screening system. Return the persisted alert with a
   server-generated `id` and timestamps. Initial `status` is `OPEN`.
2. **List alerts** — return alerts filtered by tenant (always required), and optionally by `status` and a minimum
   `matchScore`.
3. **Submit a decision** — record a `CLEARED` or `CONFIRMED_HIT` outcome with a `decisionNote`. The decision is **write-once**: once an alert is decided, it cannot be re-decided or transitioned further; attempts to re-decide must return `409 Conflict`.
4. **Escalate an alert** — transition status to `ESCALATED` and emit a domain event (see below). Only valid when current
   status is `OPEN`.

#### Event Publishing

When an alert is **escalated** or **decided**, publish a domain event representing the transition. The event must carry,
at minimum: event type, alert id, tenant id, the transition outcome, and a timestamp. Example payload (decision event):

```json
{
    "event": "alert.decided",
    "alertId": "...",
    "tenantId": "...",
    "decision": "CLEARED",
    "timestamp": "..."
}
```

The choice of publisher implementation is yours — a stdout/log publisher is fine, as is a real broker (NATS, Kafka, RabbitMQ, SQS, etc.). Design and implementation quality both matter.

#### Constraints & Rules

- Alerts must be **strictly tenant-isolated**. Decide how tenant identity is carried through requests and where isolation is enforced. Document and defend the choice in your
  README. A request whose tenant cannot be resolved must be rejected with an appropriate status.
- Use **in-memory storage** (no database required) — but structure the code as if a real DB will replace it later
  (repository pattern).

---

### What We're Looking For

| Area                              | What we'll evaluate                                                                                                                                 |
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| **Code structure**                | Clean separation of concerns (handler → service → repository), correct framework usage (if applicable)                                              |
| **Domain logic**                  | Correct state machine transitions, immutability of decisions                                                                                        |
| **API design**                    | Proper REST endpoints, HTTP status codes, error responses and input validation                                                                      |
| **Multi-tenancy**                 | How is tenant identity carried through the request? Where (and in how many places) is isolation enforced? What threat model does the design assume? |
| **Event-driven thinking**         | Domain event modeling; quality of the publisher's design and implementation                                                                         |
| **Testability**                   | Sufficient test coverage of the key paths, on both unit and integration test levels                                                                 |
| **Code quality**                  | Naming, error handling, idiomatic patterns                                                                                                          |

---

### Deliverables

1. A **Git repository** with the solution
2. A **`README.md`** with:
    - How to run the service
    - Design decisions you made and why, any compromises or deviations made should be clearly stated
    - What you would change/add for production (database, auth, message broker, etc.)
3. Be prepared to walk through the code in a **30-minute code review session** where we'll discuss trade-offs, ask "what
   if" questions, and explore how this would evolve in a real system

---

### Time Expectation

**~8-10 hours.** Don't over-engineer — we value clarity and correctness over completeness.

Good luck!
