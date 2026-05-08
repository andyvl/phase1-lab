/**
 * Reusable check bundles for the Ticketing System API.
 *
 * Each function wraps k6's check() and returns the boolean result so callers
 * can branch on it (e.g. only fetch a booking when the create succeeded).
 *
 * Usage:
 *   import { checkVenueCreated, checkBookingOk, checkConflict } from '../lib/checks.js';
 *   checkVenueCreated(res);
 */

import { check } from 'k6';

// ---------------------------------------------------------------------------
// Venue
// ---------------------------------------------------------------------------

export function checkVenueCreated(res) {
  return check(res, {
    'venue: status 201':        (r) => r.status === 201,
    'venue: has id':            (r) => r.json('id') !== undefined,
    'venue: has name':          (r) => typeof r.json('name') === 'string',
    'venue: response < 500ms':  (r) => r.timings.duration < 500,
  });
}

export function checkVenueFound(res) {
  return check(res, {
    'venue: status 200':       (r) => r.status === 200,
    'venue: has seats array':  (r) => Array.isArray(r.json('seats')),
  });
}

export function checkSeatsAdded(res) {
  return check(res, {
    'seats: status 200':        (r) => r.status === 200,
    'seats: array non-empty':   (r) => (r.json('seats') || []).length > 0,
  });
}

// ---------------------------------------------------------------------------
// Show
// ---------------------------------------------------------------------------

export function checkShowScheduled(res) {
  return check(res, {
    'show: status 201':               (r) => r.status === 201,
    'show: has id':                   (r) => r.json('id') !== undefined,
    'show: status is SCHEDULED':      (r) => r.json('status.type') === 'SCHEDULED',
    'show: seats match venue layout': (r) => (r.json('seats') || []).length > 0,
    'show: response < 500ms':         (r) => r.timings.duration < 500,
  });
}

export function checkShowOpened(res) {
  return check(res, {
    'show open: status 200':     (r) => r.status === 200,
    'show open: status is OPEN': (r) => r.json('status.type') === 'OPEN',
  });
}

export function checkShowFound(res) {
  return check(res, {
    'get show: status 200':         (r) => r.status === 200,
    'get show: has seats':          (r) => (r.json('seats') || []).length > 0,
    'get show: response < 500ms':   (r) => r.timings.duration < 500,
  });
}

// ---------------------------------------------------------------------------
// Booking — success path
// ---------------------------------------------------------------------------

export function checkBookingCreated(res) {
  return check(res, {
    'booking: status 201':       (r) => r.status === 201,
    'booking: has bookingId':    (r) => r.json('bookingId') !== undefined,
    'booking: has totalAmount':  (r) => r.json('totalAmount') !== undefined,
    'booking: response < 800ms': (r) => r.timings.duration < 800,
  });
}

export function checkBookingFound(res) {
  return check(res, {
    'get booking: status 200':         (r) => r.status === 200,
    'get booking: status CONFIRMED':   (r) => r.json('status') === 'CONFIRMED',
    'get booking: has seats':          (r) => (r.json('seats') || []).length > 0,
    'get booking: response < 500ms':   (r) => r.timings.duration < 500,
  });
}

export function checkBookingCancelled(res) {
  return check(res, {
    'cancel: status 200 or 204': (r) => r.status === 200 || r.status === 204,
    'cancel: has bookingId':     (r) => r.json('bookingId') !== undefined,
    'cancel: response < 500ms':  (r) => r.timings.duration < 500,
  });
}

// ---------------------------------------------------------------------------
// Booking — expected error paths (these passing means the API is correct)
// ---------------------------------------------------------------------------

/** Seat contention: another customer already took this seat. */
export function checkConflict(res) {
  return check(res, {
    'conflict: status 409':         (r) => r.status === 409,
    'conflict: has error message':  (r) => typeof r.json('detail') === 'string' || typeof r.json('message') === 'string',
  });
}

/** Show is not open for booking (still SCHEDULED or already CANCELLED). */
export function checkNotBookable(res) {
  return check(res, {
    'not-bookable: status 422':  (r) => r.status === 422,
  });
}

/** Resource not found. */
export function checkNotFound(res) {
  return check(res, {
    'not-found: status 404':  (r) => r.status === 404,
  });
}
