package com.example.ticketing.domain.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.ShowId;
import com.example.ticketing.domain.show.ShowSeat;
import com.example.ticketing.domain.venue.SeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookingTest {
    @Test
    void create_withValidSeats_calculatesTotal() {
        var booking = Booking.create(
            ShowId.generate(),
            new Customer("Ada Lovelace", "ada@example.com"),
            List.of(showSeat(1), showSeat(2)),
            Money.of(new BigDecimal("30.00"), "EUR"));

        var total = booking.totalAmount();

        assertEquals(new BigDecimal("60.00"), total.amount());
        assertEquals("EUR", total.currency());
    }

    @Test
    void cancel_updatesStatus() {
        var booking = Booking.create(
            ShowId.generate(),
            new Customer("Ada Lovelace", "ada@example.com"),
            List.of(showSeat(1)),
            Money.of(new BigDecimal("30.00"), "EUR"));

        booking.cancel();

        assertEquals(BookingStatus.CANCELLED, booking.status());
    }

    @Test
    void bookingResult_sealedVariants_exhaustiveSwitch() {
        BookingResult result = new BookingResult.ShowNotBookable(ShowId.generate(), "Show is currently CLOSED");

        var code = switch (result) {
            case BookingResult.Success ignored -> "SUCCESS";
            case BookingResult.SeatUnavailable ignored -> "SEAT_UNAVAILABLE";
            case BookingResult.ShowNotFound ignored -> "SHOW_NOT_FOUND";
            case BookingResult.ShowNotBookable ignored -> "SHOW_NOT_BOOKABLE";
        };

        assertEquals("SHOW_NOT_BOOKABLE", code);
    }

    private ShowSeat showSeat(int number) {
        return ShowSeat.create(ShowId.generate(), SeatId.generate(), new SeatRow("A"), new SeatNumber(number));
    }
}
