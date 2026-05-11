package com.example.ticketing.domain.show;

import com.example.ticketing.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ShowOpened(UUID eventId, Instant occurredAt, ShowId showId) implements DomainEvent {
    public static ShowOpened of(ShowId showId) {
        return new ShowOpened(UUID.randomUUID(), Instant.now(), showId);
    }
}
