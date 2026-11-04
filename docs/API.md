# API reference

The API produces JSON and exposes the following endpoints under `/api/v1`.

## Submit events

`POST /api/v1/events`

The body is an array of event objects. All fields are required. `eventId` must
be a positive integer; `eventType` is `BUY`, `SELL`, or `CANCEL`; account and
security IDs must be non-blank strings. BUY and SELL quantities must be
positive. A CANCEL quantity may be zero and is ignored.

```json
[
  {"eventId":101,"eventType":"BUY","accountId":"ACC1","securityId":"SEC1","quantity":100},
  {"eventId":102,"eventType":"SELL","accountId":"ACC1","securityId":"SEC1","quantity":25},
  {"eventId":101,"eventType":"CANCEL","accountId":"ACC1","securityId":"SEC1","quantity":0}
]
```

Original BUY/SELL IDs are globally unique. A cancellation deliberately refers
to its original ID, and the original and a single cancellation may appear
together in a batch. For a cancellation, the original trade's account,
security, type, and quantity determine the affected position and reversal; the
cancellation request's account/security/quantity values do not change that.

A successful response uses the common envelope and includes the resulting
position for each affected account/security pair. Each returned position
contains its current quantity and complete accepted event history, not only
events from this request.

## Get a position

`GET /api/v1/positions/{accountId}/{securityId}`

Returns the current position and its accepted events. If the pair has not been
affected by an accepted event, the endpoint returns HTTP 404.
Account and security identifiers are case-sensitive; identifiers differing
only by letter case refer to different positions.

## Response envelope

Successful requests return HTTP 200 and a body shaped like:

```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "accountId": "ACC1",
      "securityId": "SEC1",
      "quantity": 75,
      "events": []
    }
  ],
  "status": "OK",
  "timestamp": "2026-01-01T12:00:00",
  "correlation_id": "request-correlation-id",
  "code": 200
}
```

Error responses set `success` to `false` and contain `error.localized_message`
and, when available, `error.sub_errors`. The internal error message and status
inside the error model are not serialized; the HTTP status is sent as the
response status and is also represented by the envelope's `status` and `code`.

| Situation | HTTP status |
|---|---:|
| Invalid request fields or duplicate original IDs in one batch | 400 |
| Trade/business processing failure, including unknown or already-cancelled trade | 422 |
| Requested position does not exist | 404 |
| Other framework or unexpected failures | Determined by the exception handler |

`X-Correlation-Id` may be supplied by the caller. If absent or blank, the
application creates one. The ID is returned in the same response header and
the JSON envelope's `correlation_id`.

## Interactive API documentation

When the application is running, Swagger UI is available at
`/swagger-ui/index.html`; the generated OpenAPI JSON is at `/v3/api-docs`.

Actuator exposes `/actuator/health` and `/actuator/info`. Health details are
hidden from the HTTP response.

For event ordering, cancellation, uniqueness, atomicity, and persistence
assumptions, see [README.md](../README.md#assumptions-and-implemented-behavior).
