package com.example.ticketing.domain.venue;

import com.example.ticketing.domain.shared.EntityId;
import java.util.UUID;

public record SeatId(UUID value) implements EntityId {
    public SeatId {
        if (value == null) {
            throw new IllegalArgumentException("Seat id cannot be null");
        }
    }

    public static SeatId generate() {
        return new SeatId(UUID.randomUUID());
    }

    public static SeatId of(String uuid) {
        return new SeatId(UUID.fromString(uuid));
    }
}
