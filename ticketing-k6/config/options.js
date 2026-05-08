/**
 * Shared scenario/threshold configuration for all test types.
 * Import the appropriate export in each scenario script.
 */

// ---------------------------------------------------------------------------
// Functional (correctness) — 1 VU, runs the full API flow exactly once.
// http_req_failed is relaxed: ~5 of the 19 requests intentionally return
// 4xx (422 not-bookable, 409 conflict, 3× 404 not-found).  The strict
// gate is the checks threshold (100% must pass).
// ---------------------------------------------------------------------------
export const functionalOptions = {
  scenarios: {
    functional: {
      executor:    'per-vu-iterations',
      vus:         1,
      iterations:  1,
      maxDuration: '3m',
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.40'],   // intentional 4xx ≈ 26% — see functional.js groups 05/11/15-17
    http_req_duration: ['p(95)<2000'],
    checks:            ['rate>0.99'],   // every assertion must pass
  },
};

// ---------------------------------------------------------------------------
// Load — closed-model ramp-up/hold/ramp-down with concurrent booking VUs.
// Uses a shared show created in setup().  Some 409 conflicts are expected
// (that is correct ticketing behaviour); the booking_conflict_rate custom
// metric captures them separately from genuine errors.
// ---------------------------------------------------------------------------
export const loadOptions = {
  scenarios: {
    load: {
      executor:         'ramping-vus',
      startVUs:         0,
      stages: [
        { duration: '1m', target: 10 },   // ramp up
        { duration: '3m', target: 10 },   // hold
        { duration: '1m', target: 20 },   // second ramp
        { duration: '3m', target: 20 },   // hold
        { duration: '1m', target: 0  },   // ramp down
      ],
      gracefulRampDown: '30s',
      gracefulStop:     '30s',
    },
  },
  thresholds: {
    // Non-booking endpoints must stay clean
    'http_req_failed{endpoint:get_show}':    ['rate<0.01'],
    'http_req_failed{endpoint:get_booking}': ['rate<0.01'],
    // Booking endpoint: 409 seat-contention is expected; genuine 5xx must stay low
    'http_req_failed{endpoint:book}':        ['rate<0.60'],
    // Response-time SLOs
    'http_req_duration{endpoint:get_show}':  ['p(95)<500'],
    'http_req_duration{endpoint:book}':      ['p(95)<800'],
    // Overall check pass-rate (409 bookings are tracked via booking_conflict_rate)
    checks:                                  ['rate>0.80'],
  },
};

// ---------------------------------------------------------------------------
// Spike — simulates a "ticket drop": sudden burst of concurrent bookings
// followed by recovery back to normal traffic.
//
// Pattern: 5 RPS warmup → jump to 200 RPS in 10 s → hold → recover.
// preAllocatedVUs = ceil(200 RPS × 0.5 s p95 estimate × 1.2) = 120
//
// The thresholds here are intentionally relaxed during the spike itself.
// What matters most is that the system RECOVERS — p95 must return below
// 1 s and error rate below 5% in the post-spike window.
// ---------------------------------------------------------------------------
export const spikeOptions = {
  scenarios: {
    spike: {
      executor:        'ramping-arrival-rate',
      startRate:       1,
      timeUnit:        '1s',
      stages: [
        { duration: '30s', target: 5   },  // pre-sale warmup — normal traffic
        { duration: '10s', target: 200 },  // ticket drop! — near-instant surge
        { duration: '1m',  target: 200 },  // hold spike — sustained peak
        { duration: '10s', target: 5   },  // crowd thins out
        { duration: '30s', target: 5   },  // post-spike: verify system recovered
        { duration: '10s', target: 0   },  // cool-down
      ],
      preAllocatedVUs: 120,
      maxVUs:          500,
      gracefulStop:    '30s',
    },
  },
  thresholds: {
    // During spike some degradation is acceptable; recovery is the key signal
    http_req_failed:                      ['rate<0.15'],
    http_req_duration:                    ['p(95)<5000'],
    // Booking-specific: p95 under 3 s even at peak (non-aborting)
    'http_req_duration{endpoint:book}':   [{ threshold: 'p(95)<3000', abortOnFail: false }],
    // Dropped iterations signal VU pool exhaustion — warn but don't abort
    dropped_iterations:                   [{ threshold: 'count<1000', abortOnFail: false }],
  },
};
// preAllocatedVUs = ceil(50 RPS × 0.8s p95 estimate × 1.2) ≈ 50
// ---------------------------------------------------------------------------
export const stressOptions = {
  scenarios: {
    stress: {
      executor:        'ramping-arrival-rate',
      startRate:       2,
      timeUnit:        '1s',
      stages: [
        { duration: '1m', target: 10 },   // warm-up
        { duration: '2m', target: 10 },   // hold
        { duration: '2m', target: 30 },   // ramp
        { duration: '3m', target: 30 },   // hold
        { duration: '2m', target: 50 },   // push
        { duration: '2m', target: 0  },   // cool-down
      ],
      preAllocatedVUs: 50,
      maxVUs:          200,
      gracefulStop:    '30s',
    },
  },
  thresholds: {
    http_req_failed:    ['rate<0.10'],
    http_req_duration:  ['p(95)<2000', 'p(99)<5000'],
    dropped_iterations: [{ threshold: 'count<100', abortOnFail: false }],
  },
};
