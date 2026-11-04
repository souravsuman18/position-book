# Repository guide

## Build and test

- Use JDK 21: `pom.xml` targets Java 21, and event groups are processed with virtual threads.
- Run the full test suite with `mvn test`.
- Run one test class with `mvn -Dtest=PositionBookServiceTest test` or `mvn -Dtest=PositionControllerIntegrationTest test`.
- Run one test method with `mvn -Dtest=PositionBookServiceTest#rejectsQuantityOverflowWithoutChangingTheCommittedPosition test`.
- Start the application with `mvn spring-boot:run`.
- Swagger UI is available at `/swagger-ui/index.html`; OpenAPI JSON is at `/v3/api-docs`. Actuator exposes `/actuator/health` and `/actuator/info`.
- Generate JavaDoc with `mvn javadoc:javadoc`; output is under `target/site/apidocs/`.
- Run static analysis with `mvn checkstyle:check`, `mvn pmd:check`, and `mvn spotbugs:check`; `mvn verify` runs these and generates JaCoCo coverage.
- JaCoCo HTML coverage is at `target/site/jacoco/index.html` after `mvn verify`.
- Static-analysis XML reports are written to `target/checkstyle-result.xml`, `target/pmd.xml`, and `target/spotbugsXml.xml`; review findings there.

## Architecture

`PositionController` exposes `POST /api/v1/events` and `GET /api/v1/positions/{accountId}/{securityId}`. Requests are validated by Jakarta Bean Validation, converted from `TradeEventRequest` to `TradeEvent` by `TradeMapper`, and passed to `IPositionBookService`.

`PositionBookService` owns the in-memory state: positions keyed by `(accountId, securityId)`, original trades keyed globally by event ID, and cancellation records keyed by original event ID. A POST accepts a batch. Events for a position are processed in event-ID order and independent position groups run on virtual threads. Each group stages changes against the same snapshot; the service replaces the immutable `State` snapshot only if every group succeeds, so a failed group leaves the whole batch uncommitted. Position event lists retain accepted event history across requests, and missing position lookups return quantity zero.

`ExceptionInterceptor` maps validation and business failures to the shared `ApiResponse` error envelope. `MDCFilter` accepts or generates an `X-Correlation-Id`, adds it to the response and logging context, and removes it from the thread-local context after the request.

## Repository-specific behavior

- Original BUY/SELL IDs are globally unique, including across account/security pairs; IDs are expected to arrive incrementally. A CANCEL intentionally reuses its original trade ID, and at most one cancellation for that ID may be processed.
- Cancellation reverses the original trade using its account, security, type, and quantity. The cancellation request's security and quantity do not determine the reversal.
- BUY increases and SELL decreases quantity; short (negative) positions are allowed. BUY/SELL quantities must be positive, and arithmetic overflow is rejected.
- Account and security identifiers are case-sensitive and form the exact composite key; do not normalize their case.
- Batch errors are aggregated into `TradeProcessingException` and surfaced as HTTP 422. Duplicate original IDs in a submitted batch are rejected as HTTP 400; state-changing validation or processing failures must not partially commit other groups.
- `PositionBookServiceTest` exercises business rules directly. `PositionControllerIntegrationTest` boots Spring and uses MockMvc to verify HTTP behavior and JSON error/response shapes.
- Keep documented behavior and assumptions synchronized with `README.md` and the relevant references under `docs/`, including full accepted history in returned positions.
