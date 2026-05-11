package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.ShowSeat;
import com.example.ticketing.domain.show.ShowSeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;

public final class BookingLine {
    private final BookingLineId id;
    private final BookingId bookingId;
    private final ShowSeatId showSeatId;
    private final SeatRow row;
    private final SeatNumber number;
    private final Money price;

    private BookingLine(BookingLineId id, BookingId bookingId, ShowSeatId showSeatId, SeatRow row, SeatNumber number, Money price) {
        this.id = id;
        this.bookingId = bookingId;
        this.showSeatId = showSeatId;
        this.row = row;
        this.number = number;
        this.price = price;
    }

    public static BookingLine create(BookingId bookingId, ShowSeat seat, Money price) {
        return new BookingLine(BookingLineId.generate(), bookingId, seat.id(), seat.row(), seat.number(), price);
    }

    public static BookingLine restore(BookingLineId id, BookingId bookingId, ShowSeatId showSeatId,
                                      SeatRow row, SeatNumber number, Money price) {
        return new BookingLine(id, bookingId, showSeatId, row, number, price);
    }

    public BookingLineId id() {
        return id;
    }

    public BookingId bookingId() {
        return bookingId;
    }

    public ShowSeatId showSeatId() {
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
