package com.example.ticketing.domain.show;

import com.example.ticketing.domain.venue.SeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;

public final class ShowSeat {
    private final ShowSeatId id;
    private final ShowId showId;
    private final SeatId seatId;
    private final SeatRow row;
    private final SeatNumber number;
    private ShowSeatStatus status;
    private int version;

    private ShowSeat(ShowSeatId id, ShowId showId, SeatId seatId, SeatRow row, SeatNumber number, ShowSeatStatus status, int version) {
        this.id = id;
        this.showId = showId;
        this.seatId = seatId;
        this.row = row;
        this.number = number;
        this.status = status;
        this.version = version;
    }

    public static ShowSeat create(ShowId showId, SeatId seatId, SeatRow row, SeatNumber number) {
        return new ShowSeat(ShowSeatId.generate(), showId, seatId, row, number, ShowSeatStatus.AVAILABLE, 0);
    }

    public static ShowSeat restore(ShowSeatId id, ShowId showId, SeatId seatId, SeatRow row, SeatNumber number, ShowSeatStatus status, int version) {
        return new ShowSeat(id, showId, seatId, row, number, status, version);
    }

    public void reserve() {
        if (status != ShowSeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available");
        }
        this.status = ShowSeatStatus.RESERVED;
    }

    public void confirm() {
        if (status != ShowSeatStatus.RESERVED) {
            throw new IllegalStateException("Seat must be reserved before confirmation");
        }
        this.status = ShowSeatStatus.BOOKED;
    }

    public void release() {
        this.status = ShowSeatStatus.AVAILABLE;
    }

    public ShowSeatId id() {
        return id;
    }

    public ShowId showId() {
        return showId;
    }

    public SeatId seatId() {
        return seatId;
    }

    public SeatRow row() {
        return row;
    }

    public SeatNumber number() {
        return number;
    }

    public ShowSeatStatus status() {
        return status;
    }

    public int version() {
        return version;
    }
}
