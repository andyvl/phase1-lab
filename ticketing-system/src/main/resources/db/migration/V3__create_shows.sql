CREATE TABLE shows (
    id UUID PRIMARY KEY,
    venue_id UUID NOT NULL REFERENCES venues(id),
    title VARCHAR(255) NOT NULL,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    ticket_price_amount NUMERIC(10,2) NOT NULL,
    ticket_price_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    cancel_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
