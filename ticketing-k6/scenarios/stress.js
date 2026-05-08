/**
 * Stress test for the Ticketing System API.
 *
 * Executor : ramping-arrival-rate — open-model escalation.
 * Goal     : find the saturation point of the booking endpoint by gradually
 *            increasing RPS until the p95 latency exceeds 2 s or error rate
 *            climbs above 10%.
 *
 * preAllocatedVUs formula (from config comments):
 *   ceil(50 RPS × 0.8 s estimated p95 × 1.2) = ceil(48) → 50
 *
 * Setup    : same as load.js — creates a venue with 10 rows × 20 seats
 *            (200 seats) and opens a show.  With rapid booking + immediate
 *            cancellation, the seat pool stays available throughout the run.
 *
 * Run:
 *   k6 run scenarios/stress.js
 *   k6 run --env BASE_URL=http://localhost:8080 scenarios/stress.js
 */

import http                     from 'k6/http';
import { group, check, sleep }  from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray }          from 'k6/data';

import { stressOptions }        from '../config/options.js';
import {
  createVenue, addSeats, scheduleShow, openShow,
  getShow, cancelBooking,
  BASE_URL,
} from '../lib/client.js';
import { checkShowFound, checkBookingCreated } from '../lib/checks.js';

// ---------------------------------------------------------------------------
// Block 1 — Options
// ---------------------------------------------------------------------------
export const options = stressOptions;

// ---------------------------------------------------------------------------
// Block 2 — Data
// ---------------------------------------------------------------------------
const customers = new SharedArray('customers', () =>
  JSON.parse(open('../data/customers.json'))
);

// ---------------------------------------------------------------------------
// Custom metrics
// ---------------------------------------------------------------------------
const bookingConflicts   = new Counter('booking_conflicts_total');
const bookingSuccessRate = new Rate('booking_success_rate');
const bookingDurationMs  = new Trend('booking_duration_ms', true);

// ---------------------------------------------------------------------------
// Block 3 — Setup
// ---------------------------------------------------------------------------
export function setup() {
  console.log('Stress setup: creating venue + show…');

  const venue   = createVenue('Stress Test Arena', '1 Stress Blvd, Milan');
  const venueId = venue.id;

  const rows     = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];
  const seatNums = Array.from({ length: 20 }, (_, i) => String(i + 1));
  for (const row of rows) {
    addSeats(venueId, row, seatNums);
  }

  const now      = new Date();
  const startsAt = new Date(now.getTime() + 48 * 3600 * 1000).toISOString();
  const endsAt   = new Date(now.getTime() + 48 * 3600 * 1000 + 2 * 3600 * 1000).toISOString();

  const show   = scheduleShow(venueId, 'Stress Test Show', startsAt, endsAt, 19.99, 'EUR');
  const showId = show.id;
  openShow(showId);

  const showDetails = getShow(showId).json();
  const seatIds     = (showDetails.seats || []).map(s => s.seatId);

  console.log(`Stress setup complete: show ${showId}, ${seatIds.length} seats`);
  return { showId, seatIds };
}

// ---------------------------------------------------------------------------
// Block 4 — Default function
// ---------------------------------------------------------------------------
export default function (data) {
  const { showId, seatIds } = data;
  const customer = customers[(__VU - 1) % customers.length];
  const seatId   = seatIds[Math.floor(Math.random() * seatIds.length)];

  const t0 = Date.now();

  group('Book seat (stress)', () => {
    const res = http.post(
      `${BASE_URL}/bookings`,
      JSON.stringify({
        showId,
        seatIds:       [seatId],
        customerName:  customer.name,
        customerEmail: `${__VU}.${__ITER}.${customer.email}`, // unique per iteration
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'book' } },
    );

    if (res.status === 201) {
      bookingSuccessRate.add(true);
      bookingDurationMs.add(Date.now() - t0);
      checkBookingCreated(res);

      // Immediately cancel to recycle the seat and keep the pool available
      const bookingId = res.json('bookingId');
      if (bookingId) {
        cancelBooking(bookingId);
      }
    } else if (res.status === 409) {
      bookingConflicts.add(1);
      bookingSuccessRate.add(false);
      check(res, { 'conflict: 409': (r) => r.status === 409 });
    } else {
      bookingSuccessRate.add(false);
      check(res, { 'stress book: no unexpected error': () => false });
    }
  });

  // Read the show on every other iteration to mix read/write load
  if (__ITER % 2 === 0) {
    group('Read show (stress)', () => {
      const res = getShow(showId);
      checkShowFound(res);
    });
  }

  // Minimal think time in stress mode — we want to maximise arrival rate
  sleep(0.1);
}

// ---------------------------------------------------------------------------
// Block 5 — Teardown
// ---------------------------------------------------------------------------
export function teardown(data) {
  console.log(`Stress test complete. Show ${data.showId} remains in the DB.`);
}
