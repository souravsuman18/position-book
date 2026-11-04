# Contributing

## Prerequisites

- JDK 21
- Maven 3.9+

The project targets Java 21 and uses virtual threads. The first Maven build
requires access to download dependencies.

## Build and tests

Run all unit and Spring MVC integration tests:

```bash
mvn test
```

Run one class or method:

```bash
mvn -Dtest=PositionBookServiceTest test
mvn -Dtest=PositionControllerIntegrationTest test
mvn -Dtest=PositionBookServiceTest#rejectsQuantityOverflowWithoutChangingTheCommittedPosition test
```

The service tests cover position/event rules and atomic state publication. The
controller integration tests use MockMvc to verify HTTP behavior and JSON
responses. Use the service-level test for focused business-rule cases and the
integration test when changing request validation, endpoint behavior, or error
mapping.

Start the application locally with `mvn spring-boot:run`. Generate JavaDoc
with `mvn javadoc:javadoc`; output is written to `target/site/apidocs/`.
While running, Swagger UI is at `/swagger-ui/index.html`, the OpenAPI
specification is at `/v3/api-docs`, and Actuator exposes `/actuator/health` and
`/actuator/info`.

## Static analysis and coverage

The Maven `verify` phase runs Checkstyle, PMD, and SpotBugs in addition to
generating a JaCoCo report. Findings are currently reported without failing the
build, so they can be reviewed and addressed incrementally. The reports are
diagnostic rather than a clean-build gate; review them when changing code.

```bash
mvn checkstyle:check
mvn pmd:check
mvn spotbugs:check
mvn verify
```

JaCoCo HTML coverage is generated at `target/site/jacoco/index.html` by
`mvn verify`.
Checkstyle, PMD, and SpotBugs XML reports are written under `target/` as
`checkstyle-result.xml`, `pmd.xml`, and `spotbugsXml.xml`, respectively.

## Behavior and documentation

Read [README.md](README.md) for project assumptions and
[docs/API.md](docs/API.md) for the HTTP contract. Preserve global original
event-ID uniqueness, cancellation semantics, and all-or-nothing batch
publication when changing event processing. If behavior changes, update the
API reference and README assumptions alongside the implementation and tests.
