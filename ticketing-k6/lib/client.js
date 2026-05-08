/**
 * Thin HTTP client for the Ticketing System API.
 *
 * Centralises the base URL, default headers, and endpoint tagging so that
 * all scenario scripts stay focused on flow logic rather than plumbing.
 *
 * Usage:
 *   import { createVenue, addSeats, scheduleShow, openShow,
 *            getShow, bookSeats, getBooking, cancelBooking } from '../lib/client.js';
 */

import http from 'k6/http';
import { fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const JSON_HEADERS = {
  'Content-Type': 'application/json',
  'Accept':       'application/json',
};

// ---------------------------------------------------------------------------
// Low-level wrappers
// ---------------------------------------------------------------------------

export function post(path, body, tags = {}) {
  return http.post(
    `${BASE_URL}${path}`,
    JSON.stringify(body),
    { headers: JSON_HEADERS, tags },
  );
}

export function get(path, tags = {}) {
  return http.get(
    `${BASE_URL}${path}`,
    { headers: { Accept: 'application/json' }, tags },
  );
}

export function del(path, tags = {}) {
  return http.del(
    `${BASE_URL}${path}`,
    null,
    { headers: { Accept: 'application/json' }, tags },
  );
}

// ---------------------------------------------------------------------------
// Venue endpoints
// ---------------------------------------------------------------------------

/**
 * POST /venues — creates a new venue, aborts the test on failure.
 * @returns {Object} parsed venue JSON
 */
export function createVenue(name, address) {
  const res = post('/venues', { name, address }, { endpoint: 'create_venue' });
  if (res.status !== 201) {
    fail(`createVenue failed: HTTP ${res.status} — ${res.body}`);
  }
  return res.json();
}

/**
 * POST /venues/{venueId}/seats — adds a row of seats to a venue.
 * @param {string[]} seatNumbers  e.g. ['1','2','3']
 * @returns {Object} updated venue JSON
 */
export function addSeats(venueId, rowLabel, seatNumbers) {
  const res = post(
    `/venues/${venueId}/seats`,
    { rowLabel, seatNumbers },
    { endpoint: 'add_seats' },
  );
  if (res.status !== 200) {
    fail(`addSeats failed (venue ${venueId}, row ${rowLabel}): HTTP ${res.status} — ${res.body}`);
  }
  return res.json();
}

export function getVenue(venueId) {
  return get(`/venues/${venueId}`, { endpoint: 'get_venue' });
}

// ---------------------------------------------------------------------------
// Show endpoints
// ---------------------------------------------------------------------------

/**
 * POST /shows — schedules a new show.  Aborts on failure.
 * @returns {Object} parsed show JSON
 */
export function scheduleShow(venueId, title, startsAt, endsAt, priceAmount, priceCurrency = 'EUR') {
  const res = post('/shows', {
    venueId,
    title,
    startsAt,
    endsAt,
    ticketPriceAmount:   priceAmount,
    ticketPriceCurrency: priceCurrency,
  }, { endpoint: 'schedule_show' });
  if (res.status !== 201) {
    fail(`scheduleShow failed: HTTP ${res.status} — ${res.body}`);
  }
  return res.json();
}

/** POST /shows/{showId}/open — transitions show to OPEN state.  Aborts on failure. */
export function openShow(showId) {
  const res = post(`/shows/${showId}/open`, {}, { endpoint: 'open_show' });
  if (res.status !== 200) {
    fail(`openShow failed (show ${showId}): HTTP ${res.status} — ${res.body}`);
  }
  return res.json();
}

/** GET /shows/{showId} — returns the raw k6 response (caller handles checks). */
export function getShow(showId) {
  return get(`/shows/${showId}`, { endpoint: 'get_show' });
}

/** GET /shows — lists all shows; returns raw k6 response. */
export function listShows() {
  return get('/shows', { endpoint: 'list_shows' });
}

// ---------------------------------------------------------------------------
// Booking endpoints
// ---------------------------------------------------------------------------

/**
 * POST /bookings — attempts to book seats.
 * Returns the raw k6 response so callers can distinguish 201 vs 409.
 */
export function bookSeats(showId, seatIds, customerName, customerEmail) {
  return post('/bookings', {
    showId,
    seatIds,
    customerName,
    customerEmail,
  }, { endpoint: 'book' });
}

/** GET /bookings/{bookingId} — returns raw k6 response. */
export function getBooking(bookingId) {
  return get(`/bookings/${bookingId}`, { endpoint: 'get_booking' });
}

/** DELETE /bookings/{bookingId} — cancels a booking; returns raw k6 response. */
export function cancelBooking(bookingId) {
  return del(`/bookings/${bookingId}`, { endpoint: 'cancel_booking' });
}
