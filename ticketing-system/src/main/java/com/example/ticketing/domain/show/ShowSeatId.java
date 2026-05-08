package com.example.ticketing.domain.show;

import com.example.ticketing.domain.shared.EntityId;
import java.util.UUID;

public record ShowSeatId(UUID value) implements EntityId {
    public ShowSeatId {
        if (value == null) {
            throw new IllegalArgumentException("Show seat id cannot be null");
        }
    }

    public static ShowSeatId generate() {
        return new ShowSeatId(UUID.randomUUID());
    }

    public static ShowSeatId of(String uuid) {
        return new ShowSeatId(UUID.fromString(uuid));
    }
}
