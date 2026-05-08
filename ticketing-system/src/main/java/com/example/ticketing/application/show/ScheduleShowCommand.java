package com.example.ticketing.application.show;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ScheduleShowCommand(UUID venueId, String title, LocalDateTime startsAt, LocalDateTime endsAt,
                                  BigDecimal ticketPriceAmount, String ticketPriceCurrency) {
}
