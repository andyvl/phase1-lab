package com.example.ticketing.domain.venue;

public final class Seat {
    private final SeatId id;
    private final VenueId venueId;
    private final SeatRow row;
    private final SeatNumber number;

    private Seat(SeatId id, VenueId venueId, SeatRow row, SeatNumber number) {
        this.id = id;
        this.venueId = venueId;
        this.row = row;
        this.number = number;
    }

    static Seat create(VenueId venueId, SeatRow row, SeatNumber number) {
        return new Seat(SeatId.generate(), venueId, row, number);
    }

    public static Seat restore(SeatId id, VenueId venueId, SeatRow row, SeatNumber number) {
        return new Seat(id, venueId, row, number);
    }

    public SeatId id() {
        return id;
    }

    public VenueId venueId() {
        return venueId;
    }

    public SeatRow row() {
        return row;
    }

    public SeatNumber number() {
        return number;
    }
}
