# Architecture

## Request and processing flow

```text
HTTP request
  -> MDCFilter (correlation ID)
  -> PositionController
  -> Jakarta validation / TradeMapper
  -> IPositionBookService
  -> PositionBookService
  -> ApiResponse
```

Exceptions are translated to the common JSON error envelope by
`ExceptionInterceptor`.

## Domain state

`PositionKey` is the composite identity `(accountId, securityId)`. Both
identifiers are case-sensitive and are used as supplied; for example, `ACC1`
and `acc1` identify different accounts.
`Position` stores the current signed quantity and accepted event history.
`TradeEvent` represents BUY, SELL, and CANCEL instructions.

The service owns one volatile `State` snapshot containing:

- Current positions keyed by `PositionKey`.
- Original BUY/SELL trades keyed globally by event ID, allowing cancellation
  lookup and global uniqueness checks.
- Cancellations keyed by their original event ID, preventing a second
  cancellation.

There is no database or file-backed store; all state is lost on restart.

## Batch transaction behavior

`PositionBookService.processEvents` is synchronized to serialize batch
processing and state publication. It first checks IDs and groups input by the
original trade's position key for cancellations. Within each group, events are
processed in ascending event-ID order. Independent groups are evaluated using
virtual threads against the same starting state.

Each group builds a staged position and staged trade maps without mutating the
published snapshot. If any group fails, the service raises a
`TradeProcessingException` and publishes none of the staged changes. If every
group succeeds, it builds immutable maps and replaces the state snapshot.
Readers access the current snapshot without synchronizing.

## API and error mapping

`PositionController` maps the REST endpoints to `IPositionBookService`.
`TradeMapper` converts validated request records to domain events, and
`PositionKeyMapper` forms composite position keys. `ApiResponse` wraps success
and failure payloads with timestamp, status, code, and correlation ID.

`MDCFilter` takes `X-Correlation-Id` from a request or creates one, then places
it in both the response header and SLF4J MDC. It clears the thread-local MDC
entry after the request completes.

See [API.md](API.md) for request and response examples and
[README.md](../README.md) for documented business assumptions.
