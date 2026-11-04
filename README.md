# Position Book

A Java and Spring Boot REST API for maintaining real-time security positions from BUY, SELL, and CANCEL trade events. Positions and event history are maintained in memory.

## Requirements

- JDK 21
- Maven 3.9+
- Internet access for the first build to download Maven dependencies

## Build and run

Run the test suite and package the application:

```bash
mvn test
mvn package
```

Start the application locally:

```bash
mvn spring-boot:run
```

Generate JavaDoc:

```bash
mvn javadoc:javadoc
```

JavaDoc is generated under `target/site/apidocs/`.

## API documentation and operational endpoints

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- Application information: `http://localhost:8080/actuator/info`

Only the Actuator `health` and `info` endpoints are exposed over HTTP. Health
details are hidden from the endpoint response.

## REST API

### Submit events

`POST /api/v1/events`

Submit an array of events. BUY and SELL quantities must be positive:

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H 'Content-Type: application/json' \
  -d '[
    {"eventId":21,"eventType":"BUY","accountId":"ACC1","securityId":"SEC1","quantity":100},
    {"eventId":22,"eventType":"SELL","accountId":"ACC1","securityId":"SEC1","quantity":40}
  ]'
```

A CANCEL references the original BUY or SELL by its event ID. The original trade determines the position and quantity to reverse:

```json
[
  {"eventId":21,"eventType":"CANCEL","accountId":"ACC1","securityId":"SEC1","quantity":0}
]
```

The cancellation's account, security, and quantity do not control the reversal. The API accepts a zero quantity for CANCEL; that value is ignored.

Successful POST responses contain the current position and its accepted events for each affected account/security pair. This implementation includes the full accepted event history for each pair, including events from previous requests.

### Retrieve a position

`GET /api/v1/positions/{accountId}/{securityId}`

For example:

```bash
curl http://localhost:8080/api/v1/positions/ACC1/SEC1
```

If no events have affected the pair, the API returns HTTP 404 in the standard error envelope.

### Response envelope

Successful requests return HTTP 200 in a shared response structure:

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "accountId": "ACC1",
      "securityId": "SEC1",
      "quantity": 60,
      "events": []
    }
  ],
  "status": "OK",
  "timestamp": "2026-01-01T12:00:00",
  "correlation_id": "request-correlation-id",
  "code": 200
}
```

Error responses set `success` to `false` and contain an `error` object with a client-facing `localized_message` and optional structured `sub_errors`. Input validation errors and duplicate original IDs in a submitted batch return HTTP 400. Event-processing failures return HTTP 422. Responses include an `X-Correlation-Id` header; callers may supply one, otherwise the application generates it.

See the [API reference](docs/API.md) for request constraints, error responses, and additional detail.

## Business rules and assumptions

1. Each POST accepts a batch. Within each account/security pair, events are processed in ascending event-ID order. Independent pairs are processed concurrently. Updates from a batch are published atomically only if all groups succeed.
2. Original BUY/SELL IDs are assumed to arrive incrementally and must be globally unique across accounts and securities. IDs must be positive. The service enforces positivity and uniqueness, but does not check that each new ID is numerically greater than the previous one. Reusing an original ID in a later request or for another position is rejected.
3. A CANCEL reuses the original trade's ID. A request may contain the original trade and one cancellation for it. Duplicate original IDs and multiple cancellations for the same ID in a request are rejected. An accepted trade can be cancelled once only.
4. A cancellation reverses the original trade using its original account, security, type, and quantity. Unknown trades and repeated cancellations are rejected.
5. BUY increases quantity and SELL decreases it. Negative positions (short positions) are allowed. BUY/SELL quantities must be positive; the CANCEL quantity is ignored.
6. Account and security IDs must not be blank. Arithmetic overflow is rejected rather than silently wrapping.
7. Account IDs and security IDs are case-sensitive. Identifiers differing only by letter case refer to distinct positions.
8. Position history includes accepted BUY, SELL, and CANCEL events and persists across requests while the application is running. The response includes all accepted events for the affected pair, not only events from the current POST. A GET for a pair with no position returns HTTP 404.
9. State is in memory only and is lost when the application restarts. Synchronized service processing and immutable state snapshots are a correctness-first approach for this exercise, not a high-throughput persistence or locking design.

## Production enhancements

The following are outside the scope of this exercise and would need to be considered for production deployment:

1. Authentication and authorization.
2. Durable persistence and state recovery across application restarts.
3. Production-scale batch handling, including request-size limits, backpressure, and operational handling of large workloads. Basic batch submission and atomic processing are implemented.
4. Pagination for event history and other potentially large responses.

## Tests and code quality

Run all unit and Spring MVC integration tests:

```bash
mvn test
```

Run a single test class or method:

```bash
mvn -Dtest=PositionBookServiceTest test
mvn -Dtest=PositionControllerIntegrationTest test
mvn -Dtest=PositionBookServiceTest#rejectsQuantityOverflowWithoutChangingTheCommittedPosition test
```

Run static analysis and generate coverage:

```bash
mvn checkstyle:check
mvn pmd:check
mvn spotbugs:check
mvn verify
```

`mvn verify` runs Checkstyle, PMD, and SpotBugs (the maintained FindBugs successor), then generates the JaCoCo HTML report at `target/site/jacoco/index.html`. Static-analysis findings are reported for review and currently do not fail the build; XML reports are written under `target/`.

## Developer documentation

- [API reference](docs/API.md) — request and response details.
- [Architecture](docs/ARCHITECTURE.md) — request flow, state model, and batch processing.
- [Contributing](CONTRIBUTING.md) — development setup and test commands.
- [Copilot instructions](.github/copilot-instructions.md) — repository-specific context for Copilot sessions.
