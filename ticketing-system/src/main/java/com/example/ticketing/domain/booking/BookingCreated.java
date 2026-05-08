package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record BookingCreated(UUID eventId, Instant occurredAt, UUID bookingId, UUID customerId) implements DomainEvent {
    public static BookingCreated of(UUID bookingId, UUID customerId) {
        return new BookingCreated(UUID.randomUUID(), Instant.now(), bookingId, customerId);
    }
}
