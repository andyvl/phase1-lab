package com.example.ticketing.domain.venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VenueTest {
    @Test
    void addSeat_whenValid_seatAdded() {
        var venue = Venue.create("Grand Hall", "Main Street 1");

        var seat = venue.addSeat(new SeatRow("A"), new SeatNumber(1));

        assertEquals(1, venue.seats().size());
        assertEquals(seat, venue.seats().getFirst());
    }

    @Test
    void addSeat_whenDuplicate_throwsException() {
        var venue = Venue.create("Grand Hall", "Main Street 1");
        venue.addSeat(new SeatRow("A"), new SeatNumber(1));

        assertThrows(IllegalArgumentException.class,
            () -> venue.addSeat(new SeatRow("A"), new SeatNumber(1)));
    }
}
