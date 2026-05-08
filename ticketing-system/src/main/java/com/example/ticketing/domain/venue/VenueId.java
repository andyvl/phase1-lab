package com.example.ticketing.domain.venue;

import com.example.ticketing.domain.shared.EntityId;
import java.util.UUID;

public record VenueId(UUID value) implements EntityId {
    public VenueId {
        if (value == null) {
            throw new IllegalArgumentException("Venue id cannot be null");
        }
    }

    public static VenueId generate() {
        return new VenueId(UUID.randomUUID());
    }

    public static VenueId of(String uuid) {
        return new VenueId(UUID.fromString(uuid));
    }
}
