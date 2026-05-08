package com.example.ticketing.domain.show;

import com.example.ticketing.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ShowOpened(UUID eventId, Instant occurredAt, UUID showId) implements DomainEvent {
    public static ShowOpened of(UUID showId) {
        return new ShowOpened(UUID.randomUUID(), Instant.now(), showId);
    }
}
