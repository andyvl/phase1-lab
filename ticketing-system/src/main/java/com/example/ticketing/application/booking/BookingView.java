package com.example.ticketing.application.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingView(UUID id, String customerName, String showTitle, List<BookedSeatView> seats,
                          BigDecimal totalAmount, String currency, String status, Instant createdAt) {
}
