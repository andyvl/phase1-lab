# Ticketing System

Quarkus 3.35.2 / Java 25 reactive ticketing demo using DDD.

## Prerequisites
- Docker & Docker Compose
- Java 25+ (Java 26 works for compilation targeting `--release 25`)
- Maven 3.9+

---

## Running modes

### Dev mode (recommended for development)

```bash
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock mvn quarkus:dev
```

Quarkus Dev Services auto-provisions a PostgreSQL container — no database setup needed.

| Endpoint | URL |
|---|---|
| API | http://localhost:8080 |
| Metrics (Prometheus) | http://localhost:8080/q/metrics |
| Health | http://localhost:8080/q/health |
| Dev UI | http://localhost:8080/q/dev |

> **Note:** In dev mode only the application runs. Prometheus and Grafana are **not** started — they require `docker compose up` (see below). You can still scrape `/q/metrics` manually with `curl` during load tests.

---

### Docker Compose (full stack incl. observability)

```bash
mvn package -DskipTests
docker compose up --build
```

This starts four services:

| Service | URL | Purpose |
|---|---|---|
| App | http://localhost:8080 | Ticketing API |
| Prometheus | http://localhost:9090 | Scrapes `/q/metrics` every 5 s |
| Grafana | http://localhost:3000 | Pre-built JVM + API dashboard (no login required) |
| PostgreSQL | localhost:5432 | Database |

Open **http://localhost:3000** → "Ticketing System — JVM & API" dashboard is pre-provisioned.

---

## Garbage Collector

The application uses **Generational ZGC** (`-XX:+UseZGC`).

In JDK 25 `-XX:+UseZGC` *always* means Generational ZGC — the non-generational mode was removed. ZGC is the right choice for this application because:

- **Sub-millisecond STW pauses** — G1 pauses (even 5–10 ms) block the Vert.x event-loop thread, causing latency spikes under load. ZGC's concurrent collection keeps pauses under 1 ms.
- **Generational advantage** — request processing creates many short-lived records/Uni objects; the young generation collects them cheaply without scanning the whole heap.
- **Uncommit idle memory** — `-XX:ZUncommitDelay=30` returns unused heap pages to the OS after 30 s of inactivity.

ZGC is active in `docker compose up` (flags are in the `Dockerfile`).

For **`quarkus dev`**, you can enable it by passing `JAVA_OPTS` to Maven:

```bash
JAVA_OPTS="-XX:+UseZGC -XX:ZUncommitDelay=30 -Xlog:gc*:stdout:time,uptime,level" \
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock \
mvn quarkus:dev
```

> You can confirm the active GC by checking `/q/metrics` — with G1 you will see `gc="G1 Young Generation"`; with ZGC you will see `gc="ZGC Major Cycles"` / `gc="ZGC Minor Cycles"`.



### Metrics available at `/q/metrics`

| Metric | Description |
|---|---|
| `http_server_requests_seconds` | Request rate, latency histogram, error rate (labelled by `uri`, `status`, `outcome`) |
| `jvm_memory_used_bytes{area="heap"}` | Heap usage |
| `jvm_gc_pause_seconds` | GC pause duration and frequency |
| `jvm_gc_overhead` | % CPU time spent in GC |
| `jvm_threads_live_threads` | Active thread count |
| `process_cpu_usage` | JVM process CPU |
| `vertx_eventloop_pending_tasks` | Vert.x event-loop backlog (key congestion signal) |

### Watch metrics during a load/spike test (dev mode)

```bash
# Refresh every 3 s — latency, error rate, heap, GC overhead
watch -n3 'curl -s http://localhost:8080/q/metrics | grep -E \
  "http_server_requests_seconds_(count|sum)|jvm_memory_used_bytes.*heap|jvm_gc_overhead|vertx_eventloop_pending"'
```

### Useful PromQL queries (Grafana / Prometheus UI)

```promql
# p95 booking latency over a 15 s window
histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket{uri="/bookings"}[15s])) by (le)
) * 1000

# Booking error rate (4xx/5xx)
sum(rate(http_server_requests_seconds_count{uri="/bookings", outcome=~"CLIENT_ERROR|SERVER_ERROR"}[15s]))
/ sum(rate(http_server_requests_seconds_count{uri="/bookings"}[15s]))

# Heap growth rate
rate(jvm_memory_used_bytes{area="heap"}[30s])
```

---

## API Examples

```bash
# Create venue
curl -X POST http://localhost:8080/venues \
  -H 'Content-Type: application/json' \
  -d '{"name":"Grand Hall","address":"Main Street 1"}'

# Add seats
curl -X POST http://localhost:8080/venues/<venue-id>/seats \
  -H 'Content-Type: application/json' \
  -d '{"rowLabel":"A","seatNumbers":["1","2","3"]}'

# Schedule show
curl -X POST http://localhost:8080/shows \
  -H 'Content-Type: application/json' \
  -d '{"venueId":"<venue-id>","title":"Hamlet","startsAt":"2026-06-10T19:30:00Z","endsAt":"2026-06-10T22:00:00Z","ticketPriceAmount":45.00,"ticketPriceCurrency":"EUR"}'

# Open show for booking
curl -X POST http://localhost:8080/shows/<show-id>/open \
  -H 'Content-Type: application/json' -d '{}'

# Book seats  (seatIds = seats[].seatId from the show response, not seats[].id)
curl -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -d '{"showId":"<show-id>","seatIds":["<seat-id>"],"customerName":"Ada Lovelace","customerEmail":"ada@example.com"}'

# Cancel booking
curl -X DELETE http://localhost:8080/bookings/<booking-id>
```

