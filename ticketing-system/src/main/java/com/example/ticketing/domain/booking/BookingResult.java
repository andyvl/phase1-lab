package com.example.ticketing.domain.booking;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public sealed interface BookingResult
    permits BookingResult.Success, BookingResult.SeatUnavailable,
            BookingResult.ShowNotFound, BookingResult.ShowNotBookable {

    record Success(UUID bookingId, List<UUID> seatIds, BigDecimal totalAmount, String currency) implements BookingResult {
    }

    record SeatUnavailable(UUID showId, UUID seatId) implements BookingResult {
    }

    record ShowNotFound(UUID showId) implements BookingResult {
    }

    record ShowNotBookable(UUID showId, String reason) implements BookingResult {
    }
}
