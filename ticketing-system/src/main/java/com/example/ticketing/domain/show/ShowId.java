package com.example.ticketing.domain.show;

import com.example.ticketing.domain.shared.EntityId;
import java.util.UUID;

public record ShowId(UUID value) implements EntityId {
    public ShowId {
        if (value == null) {
            throw new IllegalArgumentException("Show id cannot be null");
        }
    }

    public static ShowId generate() {
        return new ShowId(UUID.randomUUID());
    }

    public static ShowId of(String uuid) {
        return new ShowId(UUID.fromString(uuid));
    }
}
