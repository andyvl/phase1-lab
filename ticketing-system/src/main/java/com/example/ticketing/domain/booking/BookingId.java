package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.EntityId;
import java.util.UUID;

public record BookingId(UUID value) implements EntityId {
    public BookingId {
        if (value == null) {
            throw new IllegalArgumentException("Booking id cannot be null");
        }
    }

    public static BookingId generate() {
        return new BookingId(UUID.randomUUID());
    }

    public static BookingId of(String uuid) {
        return new BookingId(UUID.fromString(uuid));
    }
}
