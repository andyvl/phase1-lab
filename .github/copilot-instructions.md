# Copilot Instructions

## Monorepo layout

```
ticketing-system/   ← Quarkus 3.35.2 / Java 25 reactive DDD application
ticketing-k6/       ← k6 performance test suite targeting the running app
```

---

## ticketing-system

### Build & test commands

```bash
# Dev mode — Dev Services auto-provisions PostgreSQL (no DB setup needed)
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock mvn quarkus:dev

# Compile only
mvn compile

# Run all unit tests (domain model, 8 tests, no Docker needed)
mvn test -Dtest="*Test"

# Run integration test only (requires Docker for Testcontainers)
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock mvn test -Dtest=BookingFlowIT

# Run a single test class
mvn test -Dtest=BookingTest

# Run a single test method
mvn test -Dtest="BookingTest#create_withValidSeats_calculatesTotal"

# Build and start full stack (app + postgres + prometheus + grafana)
mvn package -DskipTests && docker compose up --build
```

### DDD layer structure

The codebase follows strict DDD layering — understand the dependency direction before adding code:

```
domain/          ← pure Java, zero framework imports
  shared/        ← AggregateRoot, DomainEvent, EntityId, Money
  venue/         ← Venue aggregate, Seat, value objects (SeatId, SeatRow, SeatNumber)
  show/          ← Show aggregate, ShowSeat, ShowStatus (sealed), ShowSeatStatus (enum)
  booking/       ← Booking aggregate, BookingLine, BookingResult (sealed), domain events

application/     ← Quarkus CDI beans (@ApplicationScoped), orchestrates domain + persistence
  venue/         ← VenueService, CreateVenueCommand, AddSeatsCommand, VenueView
  show/          ← ShowService, ScheduleShowCommand, OpenShowCommand, ShowView, ShowSeatView
  booking/       ← BookingService, BookSeatsCommand, BookingView, BookedSeatView

infrastructure/
  persistence/   ← Hibernate Reactive Panache entities (*Entity), raw UUID fields
  rest/          ← RESTEasy Reactive resources, GlobalExceptionMapper, CorrelationIdFilter
  metrics/       ← MetricsConfiguration (MeterFilter CDI bean for Micrometer)
```

### Key conventions

**Value objects on all IDs** — every entity ID in the domain layer must be a typed value object
(`ShowId`, `BookingId`, `ShowSeatId`, `BookingLineId`, etc.), all records implementing `EntityId`.
Call `.value()` only at the infrastructure boundary (JPA entity assignment, HTTP response messages).
Never pass raw `UUID` into a domain aggregate or domain event.

**Aggregate factories** — aggregates use static factory methods, not constructors:
- `SomeAggregate.create(...)` — for new aggregates (generates ID internally)
- `SomeAggregate.restore(...)` — for reconstituting from persistence (accepts existing ID)
The no-arg constructor must remain `private`.

**Domain events** — emit via `registerEvent(SomeEvent.of(...))` inside the aggregate method.
Consume via `aggregate.pullDomainEvents()` in the application service after persistence.
Events carry typed value objects (not raw UUIDs).

**Reactive stack** — all service methods return `Uni<T>`. Never block on the Vert.x event loop.
Session/transaction management is via `@WithSession` / `@WithTransaction` on the service method.
Do **not** use `@RunOnVirtualThread` — it is incompatible with Hibernate Reactive.

**Sealed `ShowStatus`** — `ShowStatus` is a sealed interface with record variants
(`Scheduled`, `Open`, `SoldOut`, `Cancelled`). Pattern-match exhaustively with `switch`.
Jackson serialises it as `{"status": {"type": "OPEN"}}` — the nested `type` field is the discriminator.

**Optimistic locking on `ShowSeat`** — `ShowSeatEntity` carries a `@Version int version` field
(column `version` in `show_seats`). Concurrent double-bookings are prevented by Hibernate's
optimistic lock; the `GlobalExceptionMapper` maps `OptimisticLockException` → HTTP 409.

**Application service ↔ persistence** — services query JPA entities directly (no repository
interfaces). Domain objects are reconstructed in-memory using `.restore(...)` only when business
logic is needed (e.g., `openShow`, `cancelBooking`). For read-only views, map entity → view record
directly without going through the domain aggregate.

**Error handling** — `BookingResult` is a sealed interface used as a return type (not exceptions)
for booking outcomes (`Success`, `SeatUnavailable`, `ShowNotFound`, `ShowNotBookable`).
The REST resource pattern-matches on variants. `NotFoundException` and `IllegalArgumentException`
thrown from services are mapped by `GlobalExceptionMapper`.

**Migrations** — Flyway handles schema. Add new SQL files as `V{N}__description.sql` in
`src/main/resources/db/migration/`. Never modify existing migration files.
`quarkus.hibernate-orm.schema-management.strategy=none` — Hibernate never touches the schema.

**Profiles** — datasource URL/credentials use `%prod.*` prefix only. Dev/test rely on
Dev Services (dev) or `PostgreSQLTestResource` / Testcontainers (integration tests).

**Metrics** — Micrometer percentile histogram configuration lives in `MetricsConfiguration.java`
(a `@Singleton` `MeterFilter` CDI bean). Do **not** use
`quarkus.micrometer.binder.http-server.percentiles` or `.record-percentiles` — those keys are
not valid and produce startup warnings.

---

## ticketing-k6

### Run commands

```bash
cd ticketing-k6

# Functional correctness (1 VU, ~30 s) — run this first after any API change
k6 run scenarios/functional.js

# Load / stress / spike
k6 run scenarios/load.js
k6 run scenarios/stress.js
k6 run scenarios/spike.js

# Override base URL
k6 run --env BASE_URL=http://other-host:8080 scenarios/functional.js

# Save results to JSON
k6 run --out json=results/functional.json scenarios/functional.js
```

### API shape — critical gotchas

These are the most common sources of broken k6 scripts after an API change:

| Field | Correct | Wrong |
|---|---|---|
| Show status in JSON | `r.json('status.type') === 'OPEN'` | `r.json('status') === 'OPEN'` |
| Seat ID for booking | `seats[].seatId` (venue seat UUID) | `seats[].id` (show-seat UUID) |
| POST /bookings response | `r.json('bookingId')` | `r.json('id')` |
| GET /bookings/{id} response | has `seats[]`, `customerName` | has no `customerEmail` |

### Project structure

- `config/options.js` — executor + threshold definitions (`functionalOptions`, `loadOptions`, `stressOptions`, `spikeOptions`)
- `lib/client.js` — HTTP wrappers for every endpoint with endpoint tagging; import from here, do not hand-write `http.get/post` calls in scenarios
- `lib/checks.js` — reusable check bundles per operation
- `data/customers.json` — 20 test customers loaded via `SharedArray`

### Thresholds note

The functional scenario intentionally makes ~26% 4xx requests (422 not-bookable, 409 conflict,
3× 404). The `http_req_failed` threshold is `rate<0.40` — do not tighten it to `<0.01`.
The correctness gate is `checks: rate>0.99`.
