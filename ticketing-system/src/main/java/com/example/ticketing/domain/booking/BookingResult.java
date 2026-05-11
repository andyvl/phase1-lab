package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.show.ShowId;
import com.example.ticketing.domain.venue.SeatId;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public sealed interface BookingResult
    permits BookingResult.Success, BookingResult.SeatUnavailable,
            BookingResult.ShowNotFound, BookingResult.ShowNotBookable {

    record Success(UUID bookingId, List<UUID> seatIds, BigDecimal totalAmount, String currency) implements BookingResult {
    }

    record SeatUnavailable(ShowId showId, UUID seatId) implements BookingResult {
    }

    record ShowNotFound(ShowId showId) implements BookingResult {
    }

    record ShowNotBookable(ShowId showId, String reason) implements BookingResult {
    }
}
