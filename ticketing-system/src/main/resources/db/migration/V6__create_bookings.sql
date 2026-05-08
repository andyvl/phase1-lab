CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    show_id UUID NOT NULL REFERENCES shows(id),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    total_amount NUMERIC(10,2) NOT NULL,
    total_currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
