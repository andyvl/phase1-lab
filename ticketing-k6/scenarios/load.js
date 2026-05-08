/**
 * Load test for the Ticketing System API.
 *
 * Executor : ramping-vus — closed-model concurrent booking simulation.
 * Goal     : measure throughput, latency, and contention-handling under
 *            sustained concurrent load.
 *
 * Setup    : creates one venue (10 rows × 20 seats = 200 seats) and opens a
 *            show so VUs can immediately start booking.
 *
 * VU flow  :
 *   1. Pick a random seat from the shared pool.
 *   2. Attempt to book it.
 *      → 201 CONFIRMED  : get the booking, then cancel it (frees the seat
 *                         so the pool never fully exhausts during the run).
 *      → 409 CONFLICT   : counted as expected contention, not an error.
 *      → anything else  : counted as a genuine failure.
 *   3. Read the show to see current seat availability.
 *   4. Think time 1–2 s.
 *
 * Thresholds are set in config/options.js::loadOptions.
 *
 * Run:
 *   k6 run scenarios/load.js
 *   k6 run --env BASE_URL=http://localhost:8080 scenarios/load.js
 */

import http                   from 'k6/http';
import { group, check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray }         from 'k6/data';

import { loadOptions }         from '../config/options.js';
import {
  createVenue, addSeats, scheduleShow, openShow,
  getShow, bookSeats, getBooking, cancelBooking,
  BASE_URL,
} from '../lib/client.js';
import {
  checkShowFound,
  checkBookingCreated, checkBookingFound, checkBookingCancelled,
} from '../lib/checks.js';

// ---------------------------------------------------------------------------
// Block 1 — Options
// ---------------------------------------------------------------------------
export const options = loadOptions;

// ---------------------------------------------------------------------------
// Block 2 — Data
// ---------------------------------------------------------------------------
const customers = new SharedArray('customers', () =>
  JSON.parse(open('../data/customers.json'))
);

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------

/** How many times VUs hit a seat-conflict (409) — expected for a ticketing system */
const bookingConflicts = new Counter('booking_conflicts_total');

/** Rate of successful bookings (201) vs total booking attempts */
const bookingSuccessRate = new Rate('booking_success_rate');

/** End-to-end booking latency (create → confirm) */
const bookingDurationMs = new Trend('booking_duration_ms', true);

// ---------------------------------------------------------------------------
// Block 3 — Setup
// ---------------------------------------------------------------------------
export function setup() {
  console.log('Setup: creating venue + show for load test…');

  const venue = createVenue('Load Test Arena', '99 Performance Blvd, Rome');
  const venueId = venue.id;

  // 10 rows × 20 seats = 200 seats
  const rows = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];
  const seatNums = Array.from({ length: 20 }, (_, i) => String(i + 1));

  for (const row of rows) {
    addSeats(venueId, row, seatNums);
  }

  const now      = new Date();
  const startsAt = new Date(now.getTime() + 24 * 3600 * 1000).toISOString();
  const endsAt   = new Date(now.getTime() + 24 * 3600 * 1000 + 2 * 3600 * 1000).toISOString();

  const show   = scheduleShow(venueId, 'Load Test Show', startsAt, endsAt, 25.00, 'EUR');
  const showId = show.id;

  openShow(showId);

  // Collect all seat UUIDs from the show (these are venue seat IDs, which is
  // what the booking API accepts)
  const showDetails = getShow(showId).json();
  const seatIds     = (showDetails.seats || []).map(s => s.seatId);

  console.log(`Setup complete: venue ${venueId}, show ${showId}, ${seatIds.length} seats`);
  return { showId, seatIds };
}

// ---------------------------------------------------------------------------
// Block 4 — Default function (VU workload)
// ---------------------------------------------------------------------------
export default function (data) {
  const { showId, seatIds } = data;
  const customer = customers[(__VU - 1) % customers.length];

  // Each VU picks a random seat — creates natural contention between VUs
  const seatId = seatIds[Math.floor(Math.random() * seatIds.length)];

  let bookingId = null;
  const t0      = Date.now();

  group('Book seat', () => {
    const res = http.post(
      `${BASE_URL}/bookings`,
      JSON.stringify({
        showId,
        seatIds:       [seatId],
        customerName:  customer.name,
        customerEmail: customer.email,
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'book' } },
    );

    if (res.status === 201) {
      bookingSuccessRate.add(true);
      bookingDurationMs.add(Date.now() - t0);
      checkBookingCreated(res);
      bookingId = res.json('bookingId');
    } else if (res.status === 409) {
      // Seat already taken — correct behaviour for a ticketing system
      bookingConflicts.add(1);
      bookingSuccessRate.add(false);
      check(res, { 'conflict: 409 response': (r) => r.status === 409 });
    } else {
      // Unexpected error — record as failure
      bookingSuccessRate.add(false);
      check(res, { 'booking: unexpected error': () => false });
      console.warn(`Unexpected booking response: HTTP ${res.status}`);
    }
  });

  // If booking succeeded: verify it, then cancel to recycle the seat
  if (bookingId !== null) {
    group('Get booking', () => {
      const res = getBooking(bookingId);
      checkBookingFound(res);
    });

    sleep(Math.random() + 0.5);   // simulate the user looking at their confirmation

    group('Cancel booking (recycle seat)', () => {
      const res = cancelBooking(bookingId);
      checkBookingCancelled(res);
    });
  }

  // Always read the show — exercises the read path under concurrent writes
  group('Read show availability', () => {
    const res = getShow(showId);
    checkShowFound(res);
  });

  sleep(Math.random() + 1);   // 1–2 s think time between iterations
}

// ---------------------------------------------------------------------------
// Block 5 — Teardown
// ---------------------------------------------------------------------------
export function teardown(data) {
  console.log(`Load test complete. Show ${data.showId} will remain in the DB.`);
  console.log('To reset, restart the application or drop + recreate the database.');
}
