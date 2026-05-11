package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record BookingCreated(UUID eventId, Instant occurredAt, BookingId bookingId, CustomerId customerId) implements DomainEvent {
    public static BookingCreated of(BookingId bookingId, CustomerId customerId) {
        return new BookingCreated(UUID.randomUUID(), Instant.now(), bookingId, customerId);
    }
}
