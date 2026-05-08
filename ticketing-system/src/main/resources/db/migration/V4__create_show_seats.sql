CREATE TABLE show_seats (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL REFERENCES shows(id),
    seat_id UUID NOT NULL REFERENCES seats(id),
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version INTEGER NOT NULL DEFAULT 0,
    UNIQUE (show_id, seat_id)
);
CREATE INDEX idx_show_seats_show_id ON show_seats(show_id);
CREATE INDEX idx_show_seats_status ON show_seats(show_id, status);
