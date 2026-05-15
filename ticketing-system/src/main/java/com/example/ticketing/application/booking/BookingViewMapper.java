package com.example.ticketing.application.booking;

import com.example.ticketing.infrastructure.persistence.BookingEntity;
import com.example.ticketing.infrastructure.persistence.BookingLineEntity;
import com.example.ticketing.infrastructure.persistence.CustomerEntity;
import com.example.ticketing.infrastructure.persistence.ShowEntity;

import java.util.Comparator;
import java.util.List;

/**
 * Maps persistence entities to API view/DTO objects.
 */
class BookingViewMapper {

    BookingView toBookingView(BookingEntity booking, CustomerEntity customer, ShowEntity show,
                              List<BookingLineEntity> lines) {
        return new BookingView(
                booking.id,
                customer != null ? customer.name : "Unknown",
                show != null ? show.title : "Unknown",
                lines.stream()
                        .sorted(Comparator.comparing((BookingLineEntity l) -> l.rowLabel)
                                .thenComparingInt(l -> l.seatNumber))
                        .map(l -> new BookedSeatView(l.rowLabel, l.seatNumber))
                        .toList(),
                booking.totalAmount,
                booking.totalCurrency,
                booking.status,
                booking.createdAt);
    }
}
