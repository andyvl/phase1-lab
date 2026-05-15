package com.example.ticketing.domain.show;

import java.util.UUID;

public class SeatUnavailableException extends RuntimeException {
    private final ShowId showId;
    private final UUID seatId;

    public SeatUnavailableException(ShowId showId, UUID seatId) {
        super("Seat %s for show %s is not available".formatted(seatId, showId.value()));
        this.showId = showId;
        this.seatId = seatId;
    }

    public ShowId showId() {
        return showId;
    }

    public UUID seatId() {
        return seatId;
    }
}
