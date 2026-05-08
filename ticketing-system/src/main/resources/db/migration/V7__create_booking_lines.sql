CREATE TABLE booking_lines (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings(id),
    show_seat_id UUID NOT NULL REFERENCES show_seats(id),
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    price_amount NUMERIC(10,2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL DEFAULT 'EUR'
);
