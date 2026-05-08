package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.EntityId;
import java.util.UUID;

public record CustomerId(UUID value) implements EntityId {
    public CustomerId {
        if (value == null) {
            throw new IllegalArgumentException("Customer id cannot be null");
        }
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    public static CustomerId of(String uuid) {
        return new CustomerId(UUID.fromString(uuid));
    }
}
