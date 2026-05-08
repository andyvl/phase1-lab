package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record BookingCancelled(UUID eventId, Instant occurredAt, UUID bookingId, UUID customerId) implements DomainEvent {
    public static BookingCancelled of(UUID bookingId, UUID customerId) {
        return new BookingCancelled(UUID.randomUUID(), Instant.now(), bookingId, customerId);
    }
}
