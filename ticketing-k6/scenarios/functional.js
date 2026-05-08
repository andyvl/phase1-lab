/**
 * Functional / smoke test for the Ticketing System API.
 *
 * Executor : per-vu-iterations — 1 VU, 1 iteration.
 * Goal     : verify correctness of every endpoint and business rule,
 *            not throughput.
 *
 * Flows covered:
 *   Happy path   — venue → seats → show → open → book → get → cancel → re-book
 *   Error cases  — 404 on unknown resource
 *                  422 when booking a non-open show
 *                  409 when a seat is already booked (simulated with 1 seat / 2 attempts)
 *
 * Run:
 *   k6 run scenarios/functional.js
 *   k6 run --env BASE_URL=http://localhost:8080 scenarios/functional.js
 */

import http                    from 'k6/http';
import { group, check, sleep, fail } from 'k6';
import { SharedArray }         from 'k6/data';

import { functionalOptions }   from '../config/options.js';
import {
  createVenue, addSeats,
  scheduleShow, openShow, getShow,
  bookSeats, getBooking, cancelBooking,
  getVenue, BASE_URL,
} from '../lib/client.js';
import {
  checkVenueCreated, checkVenueFound, checkSeatsAdded,
  checkShowScheduled, checkShowOpened, checkShowFound,
  checkBookingCreated, checkBookingFound, checkBookingCancelled,
  checkConflict, checkNotBookable, checkNotFound,
} from '../lib/checks.js';

// ---------------------------------------------------------------------------
// Block 1 — Options
// ---------------------------------------------------------------------------
export const options = functionalOptions;

// ---------------------------------------------------------------------------
// Block 2 — Data
// ---------------------------------------------------------------------------
const customers = new SharedArray('customers', () =>
  JSON.parse(open('../data/customers.json'))
);

// ---------------------------------------------------------------------------
// Block 3 — Setup  (not needed for functional — all state is created per VU)
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Block 4 — Default function (single VU, single iteration)
// ---------------------------------------------------------------------------
export default function () {
  const customer = customers[0];

  // ── Venue ──────────────────────────────────────────────────────────────────
  let venueId;

  group('01 Create venue', () => {
    const res = http.post(
      `${BASE_URL}/venues`,
      JSON.stringify({ name: 'Functional Test Theatre', address: '1 Test Street, Milan' }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'create_venue' } },
    );
    checkVenueCreated(res);
    if (res.status !== 201) fail(`Cannot proceed without a venue: HTTP ${res.status}`);
    venueId = res.json('id');
  });

  group('02 Add seats (3 rows × 5 seats)', () => {
    for (const row of ['A', 'B', 'C']) {
      const res = http.post(
        `${BASE_URL}/venues/${venueId}/seats`,
        JSON.stringify({
          rowLabel:    row,
          seatNumbers: ['1', '2', '3', '4', '5'],
        }),
        { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
          tags: { endpoint: 'add_seats' } },
      );
      checkSeatsAdded(res);
    }
  });

  group('03 Get venue', () => {
    const res = getVenue(venueId);
    checkVenueFound(res);
    check(res, { 'venue: 15 seats total': (r) => (r.json('seats') || []).length === 15 });
  });

  // ── Show ───────────────────────────────────────────────────────────────────
  let showId;
  let allSeatIds;

  group('04 Schedule show', () => {
    const now       = new Date();
    const startsAt  = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString();
    const endsAt    = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000 + 2 * 3600 * 1000).toISOString();

    const res = http.post(
      `${BASE_URL}/shows`,
      JSON.stringify({
        venueId,
        title:               'Functional Test Show',
        startsAt,
        endsAt,
        ticketPriceAmount:   49.99,
        ticketPriceCurrency: 'EUR',
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'schedule_show' } },
    );
    checkShowScheduled(res);
    if (res.status !== 201) fail(`Cannot proceed without a show: HTTP ${res.status}`);
    showId     = res.json('id');
    // seats[].seatId is the venue seat UUID; seats[].id is the show-seat UUID.
    // The booking API expects venue seat UUIDs.
    allSeatIds = (res.json('seats') || []).map(s => s.seatId);
  });

  // ── Error case: book a SCHEDULED (not-yet-open) show ──────────────────────
  group('05 Error: book non-open show (expect 422)', () => {
    const res = http.post(
      `${BASE_URL}/bookings`,
      JSON.stringify({
        showId,
        seatIds:       [allSeatIds[0]],
        customerName:  customer.name,
        customerEmail: customer.email,
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'book' } },
    );
    checkNotBookable(res);
  });

  // ── Open the show ──────────────────────────────────────────────────────────
  group('06 Open show', () => {
    const res = http.post(
      `${BASE_URL}/shows/${showId}/open`,
      JSON.stringify({}),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'open_show' } },
    );
    checkShowOpened(res);
    if (res.status !== 200) fail(`Cannot proceed without an open show: HTTP ${res.status}`);
  });

  group('07 Get open show', () => {
    const res = getShow(showId);
    checkShowFound(res);
    check(res, {
      'show: all seats AVAILABLE': (r) =>
        (r.json('seats') || []).every(s => s.status === 'AVAILABLE'),
    });
  });

  // ── Booking happy path ─────────────────────────────────────────────────────
  let bookingId;
  const seatsToBook = allSeatIds.slice(0, 2);   // book 2 seats

  group('08 Book seats (happy path)', () => {
    const res = http.post(
      `${BASE_URL}/bookings`,
      JSON.stringify({
        showId,
        seatIds:       seatsToBook,
        customerName:  customer.name,
        customerEmail: customer.email,
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'book' } },
    );
    checkBookingCreated(res);
    if (res.status !== 201) fail(`Booking failed unexpectedly: HTTP ${res.status} — ${res.body}`);
    bookingId = res.json('bookingId');
  });

  group('09 Get booking', () => {
    const res = getBooking(bookingId);
    checkBookingFound(res);
    check(res, {
      'booking: matches customer name': (r) => r.json('customerName') === customer.name,
      'booking: 2 seats':               (r) => (r.json('seats') || []).length === 2,
    });
  });

  group('10 Verify booked seats shown as BOOKED in show', () => {
    const res = getShow(showId);
    checkShowFound(res);
    const seats = res.json('seats') || [];
    check(res, {
      'show: booked seats are BOOKED': () =>
        seatsToBook.every(id => {
          // match by seatId (venue seat UUID), not by show-seat id
          const s = seats.find(seat => seat.seatId === id);
          return s && s.status === 'BOOKED';
        }),
      'show: remaining seats AVAILABLE': () =>
        seats.filter(s => !seatsToBook.includes(s.seatId)).every(s => s.status === 'AVAILABLE'),
    });
  });

  // ── Error case: double-book already-booked seats ───────────────────────────
  group('11 Error: double-book same seats (expect 409)', () => {
    const res = http.post(
      `${BASE_URL}/bookings`,
      JSON.stringify({
        showId,
        seatIds:       seatsToBook,
        customerName:  customers[1].name,
        customerEmail: customers[1].email,
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'book' } },
    );
    checkConflict(res);
  });

  // ── Cancel & verify seats released ────────────────────────────────────────
  group('12 Cancel booking', () => {
    const res = cancelBooking(bookingId);
    checkBookingCancelled(res);
    if (res.status !== 200 && res.status !== 204) {
      fail(`Cancel failed: HTTP ${res.status}`);
    }
  });

  group('13 Verify seats released after cancellation', () => {
    const res = getShow(showId);
    checkShowFound(res);
    const seats = res.json('seats') || [];
    check(res, {
      'show: previously booked seats now AVAILABLE': () =>
        seatsToBook.every(id => {
          const s = seats.find(seat => seat.seatId === id);
          return s && s.status === 'AVAILABLE';
        }),
    });
  });

  // ── Re-book after cancel ───────────────────────────────────────────────────
  group('14 Re-book released seats', () => {
    const res = http.post(
      `${BASE_URL}/bookings`,
      JSON.stringify({
        showId,
        seatIds:       seatsToBook,
        customerName:  customers[1].name,
        customerEmail: customers[1].email,
      }),
      { headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        tags: { endpoint: 'book' } },
    );
    checkBookingCreated(res);
  });

  // ── Error case: GET non-existent resources ─────────────────────────────────
  group('15 Error: unknown venue (expect 404)', () => {
    const res = getVenue('00000000-0000-0000-0000-000000000000');
    checkNotFound(res);
  });

  group('16 Error: unknown show (expect 404)', () => {
    const res = getShow('00000000-0000-0000-0000-000000000000');
    checkNotFound(res);
  });

  group('17 Error: unknown booking (expect 404)', () => {
    const res = getBooking('00000000-0000-0000-0000-000000000000');
    checkNotFound(res);
  });

  sleep(1);
}
