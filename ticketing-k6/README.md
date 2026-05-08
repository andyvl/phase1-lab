# ticketing-k6

k6 test suite for the [Ticketing System](../ticketing-system/) Quarkus application.

## Prerequisites

```bash
brew install k6          # macOS
# or: apt install k6 / choco install k6
```

The ticketing system must be running locally:
```bash
cd ../ticketing-system
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock mvn quarkus:dev
```

---

## Project structure

```
ticketing-k6/
├── config/
│   └── options.js          ← executor + threshold definitions (all test types)
├── data/
│   └── customers.json      ← SharedArray test customer data (20 records)
├── lib/
│   ├── client.js           ← HTTP wrappers with endpoint tagging
│   └── checks.js           ← reusable k6 check bundles
└── scenarios/
    ├── functional.js       ← correctness test  (1 VU, covers all flows + error cases)
    ├── load.js             ← load test          (ramping-vus, closed model)
    ├── spike.js            ← spike test         (ramping-arrival-rate, ticket-drop model)
    └── stress.js           ← stress test        (ramping-arrival-rate, open model)
```

---

## Running tests

All commands assume `BASE_URL` defaults to `http://localhost:8080`.  
Override with `--env BASE_URL=http://other-host:8080`.

### Functional / smoke

Verifies API correctness end-to-end (1 VU, 1 iteration, ~30 s):

```bash
k6 run scenarios/functional.js
```

Flows covered:
- `POST /venues` → 201
- `POST /venues/{id}/seats` → 200
- `GET  /venues/{id}` → 200, 15 seats
- `POST /shows` → 201 (SCHEDULED)
- `POST /bookings` on SCHEDULED show → **422** (not bookable)
- `POST /shows/{id}/open` → 200 (OPEN)
- `POST /bookings` → 201 (CONFIRMED)
- `GET  /bookings/{id}` → 200
- Seats shown BOOKED in `GET /shows/{id}`
- Double-book same seats → **409** (conflict)
- `DELETE /bookings/{id}` → 200
- Seats released → AVAILABLE again in show
- Re-book released seats → 201
- Unknown resource IDs → **404**

### Load test

Simulates concurrent users competing for seats (9 min total):

```bash
k6 run scenarios/load.js
```

Stages: 0 → 10 VUs (1 min) → hold 3 min → 20 VUs (1 min) → hold 3 min → 0 (1 min)

Key metrics emitted:
| Metric | Description |
|---|---|
| `booking_conflicts_total` | Counter — expected 409 seat-contention hits |
| `booking_success_rate`    | Rate — successful bookings / total attempts |
| `booking_duration_ms`     | Trend — end-to-end booking latency |

### Spike test

Models a "ticket drop": 5 RPS warmup → jump to **200 RPS in 10 s** → 1 min at peak → recovery (~2m30s total):

```bash
k6 run scenarios/spike.js
```

Key signals to watch:
| Metric | What it tells you |
|---|---|
| `booking_duration_ms` (p95, tagged by phase) | Latency degradation at peak vs post-spike recovery |
| `booking_conflicts_total` | Volume of seat contention at extreme concurrency |
| `dropped_iterations` | Whether the VU pool (120 pre-alloc, 500 max) was exhausted |

### Stress test

Open-model RPS escalation to find saturation point (12 min total):

```bash
k6 run scenarios/stress.js
```

Stages: 2 → 10 → 30 → 50 RPS.  
Watch `dropped_iterations` — if non-zero, the server or k6 VU pool is saturated.

---

## Output to JSON

```bash
k6 run --out json=results/functional.json scenarios/functional.js
k6 run --out json=results/load.json       scenarios/load.js
```

## Environment variables

| Variable  | Default                 | Description         |
|-----------|-------------------------|---------------------|
| `BASE_URL` | `http://localhost:8080` | Application base URL |
