/**
 * Spike test for the Ticketing System API.
 *
 * Models the "ticket drop" moment: a popular show goes on sale and thousands
 * of users simultaneously flood the booking endpoint.
 *
 * Executor : ramping-arrival-rate — open model, controlled RPS.
 *
 * Spike shape (total ~2m30s):
 *   30 s   ·   5 RPS  — pre-sale warmup, normal traffic
 *   10 s   · 200 RPS  — ticket drop, near-instant surge
 *    1 min · 200 RPS  — sustained peak (rush window)
 *   10 s   ·   5 RPS  — crowd thins out
 *   30 s   ·   5 RPS  — post-spike window (verify recovery)
 *   10 s   ·   0 RPS  — cool-down
 *
 * What to watch:
 *   - http_req_duration{endpoint:book} — does p95 stay below 3 s at peak?
 *   - booking_success_rate             — how many seats are sold successfully?
 *   - booking_conflicts_total          — contention under extreme concurrency
 *   - dropped_iterations               — VU pool saturation signal
 *   - recovery: after the spike, does p95 drop back below 500 ms?
 *
 * Setup  : venue with 10 rows × 30 seats = 300 seats, show opened.
 *          Bookings are cancelled immediately so the pool never exhausts.
 *
 * Run:
 *   k6 run scenarios/spike.js
 *   k6 run --env BASE_URL=http://localhost:8080 scenarios/spike.js
 */

import http                     from 'k6/http';
import { group, check, sleep }  from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray }          from 'k6/data';

import { spikeOptions }         from '../config/options.js';
import {
  createVenue, addSeats, scheduleShow, openShow,
  getShow, cancelBooking,
  BASE_URL,
} from '../lib/client.js';

// ---------------------------------------------------------------------------
// Block 1 — Options
// ---------------------------------------------------------------------------
export const options = spikeOptions;

// ---------------------------------------------------------------------------
// Block 2 — Data
// ---------------------------------------------------------------------------
const customers = new SharedArray('customers', () =>
  JSON.parse(open('../data/customers.json'))
);

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------

/** Seat-contention hits (409) — expected to spike during the surge window */
const bookingConflicts   = new Counter('booking_conflicts_total');

/** Ratio of confirmed bookings to total attempts */
const bookingSuccessRate = new Rate('booking_success_rate');

/** End-to-end booking latency — the primary SLO signal */
const bookingDurationMs  = new Trend('booking_duration_ms', true);

// ---------------------------------------------------------------------------
// Block 3 — Setup
// ---------------------------------------------------------------------------
export function setup() {
  console.log('Spike setup: creating venue + show…');

  const venue   = createVenue('Spike Test Stadium', 'Via della Ressa 1, Turin');
  const venueId = venue.id;

  // 10 rows × 30 seats = 300 seats — enough headroom for the peak window
  const rows     = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];
  const seatNums = Array.from({ length: 30 }, (_, i) => String(i + 1));
  for (const row of rows) {
    addSeats(venueId, row, seatNums);
  }

  const now      = new Date();
  const startsAt = new Date(now.getTime() + 72 * 3600 * 1000).toISOString();
  const endsAt   = new Date(now.getTime() + 72 * 3600 * 1000 + 2 * 3600 * 1000).toISOString();

  const show   = scheduleShow(venueId, 'Spike Test Show — Sold Out Night', startsAt, endsAt, 99.00, 'EUR');
  const showId = show.id;
  openShow(showId);

  const showDetails = getShow(showId).json();
  const seatIds     = (showDetails.seats || []).map(s => s.seatId);

  console.log(`Spike setup done: show ${showId}, ${seatIds.length} seats available`);
  return { showId, seatIds };
}

// ---------------------------------------------------------------------------
// Block 4 — Default function (intentionally thin — maximise RPS)
// ---------------------------------------------------------------------------
export default function (data) {
  const { showId, seatIds } = data;

  // Rotate customer by VU to spread load; unique email per iteration avoids
  // any per-customer uniqueness constraints
  const customer = customers[(__VU - 1) % customers.length];
  const seatId   = seatIds[Math.floor(Math.random() * seatIds.length)];

  const t0 = Date.now();

  // ── Booking attempt — the core operation under spike ────────────────────
  group('Spike book', () => {
    const res = http.post(
      `${BASE_URL}/bookings`,
      JSON.stringify({
        showId,
        seatIds:       [seatId],
        customerName:  customer.name,
        customerEmail: `vu${__VU}.it${__ITER}.${customer.email}`,
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'book' } },
    );

    if (res.status === 201) {
      bookingSuccessRate.add(true);
      bookingDurationMs.add(Date.now() - t0, { phase: currentPhase() });
      check(res, {
        'spike book: 201 confirmed': (r) => r.status === 201,
        'spike book: has bookingId': (r) => r.json('bookingId') !== undefined,
        'spike book: < 3000ms':      (r) => r.timings.duration < 3000,
      });

      // Immediately cancel — recycles the seat so the pool stays available
      // throughout the full spike window.
      const bookingId = res.json('bookingId');
      if (bookingId) {
        http.del(
          `${BASE_URL}/bookings/${bookingId}`,
          null,
          { headers: { Accept: 'application/json' },
            tags: { endpoint: 'cancel_booking' } },
        );
      }
    } else if (res.status === 409) {
      // Seat contention — expected and correct behaviour under extreme load
      bookingConflicts.add(1);
      bookingSuccessRate.add(false);
      check(res, { 'spike conflict: 409': (r) => r.status === 409 });
    } else {
      // Genuine error (5xx, 503, timeout…)
      bookingSuccessRate.add(false);
      check(res, { 'spike book: no server error': () => false });
      console.warn(`Unexpected spike response: HTTP ${res.status} — ${res.body.slice(0, 120)}`);
    }
  });

  // No sleep — spike scenarios maximise arrival rate.
  // Think time is controlled by the arrival-rate executor, not sleep().
}

// ---------------------------------------------------------------------------
// Block 5 — Teardown
// ---------------------------------------------------------------------------
export function teardown(data) {
  console.log(`Spike test complete. Show ${data.showId} remains in the DB.`);
  console.log('Key signals to review:');
  console.log('  booking_duration_ms   — p95 during peak vs post-spike recovery');
  console.log('  booking_conflicts_total — seat contention volume at peak');
  console.log('  dropped_iterations    — whether the VU pool was exhausted');
}

// ---------------------------------------------------------------------------
// Helper — label the current test phase for metric tagging
// ---------------------------------------------------------------------------
function currentPhase() {
  const elapsed = (Date.now() / 1000) % 10000;  // rough elapsed seconds
  if (elapsed < 30)  return 'warmup';
  if (elapsed < 100) return 'spike';
  if (elapsed < 150) return 'recovery';
  return 'post-spike';
}
