package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.EntityId;
import java.util.UUID;

public record BookingLineId(UUID value) implements EntityId {
    public BookingLineId {
        if (value == null) {
            throw new IllegalArgumentException("BookingLine id cannot be null");
        }
    }

    public static BookingLineId generate() {
        return new BookingLineId(UUID.randomUUID());
    }

    public static BookingLineId of(String uuid) {
        return new BookingLineId(UUID.fromString(uuid));
    }
}
