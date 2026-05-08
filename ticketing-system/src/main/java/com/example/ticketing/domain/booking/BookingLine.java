package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.ShowSeat;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;
import java.util.UUID;

public final class BookingLine {
    private final UUID id;
    private final BookingId bookingId;
    private final UUID showSeatId;
    private final SeatRow row;
    private final SeatNumber number;
    private final Money price;

    private BookingLine(UUID id, BookingId bookingId, UUID showSeatId, SeatRow row, SeatNumber number, Money price) {
        this.id = id;
        this.bookingId = bookingId;
        this.showSeatId = showSeatId;
        this.row = row;
        this.number = number;
        this.price = price;
    }

    public static BookingLine create(BookingId bookingId, ShowSeat seat, Money price) {
        return new BookingLine(UUID.randomUUID(), bookingId, seat.id().value(), seat.row(), seat.number(), price);
    }

    public UUID id() {
        return id;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public UUID showSeatId() {
        return showSeatId;
    }

    public SeatRow row() {
        return row;
    }

    public SeatNumber number() {
        return number;
    }

    public Money price() {
        return price;
    }
}
