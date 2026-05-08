CREATE TABLE seats (
    id UUID PRIMARY KEY,
    venue_id UUID NOT NULL REFERENCES venues(id),
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    UNIQUE (venue_id, row_label, seat_number)
);
