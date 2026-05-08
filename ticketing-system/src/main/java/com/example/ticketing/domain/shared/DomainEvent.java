package com.example.ticketing.domain.shared;

import com.example.ticketing.domain.booking.BookingCancelled;
import com.example.ticketing.domain.booking.BookingCreated;
import com.example.ticketing.domain.show.ShowOpened;
import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
}
