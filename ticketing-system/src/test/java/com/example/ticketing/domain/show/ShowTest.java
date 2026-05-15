package com.example.ticketing.domain.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.venue.SeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;
import com.example.ticketing.domain.venue.VenueId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShowTest {
    @Test
    void open_whenScheduled_succeeds() {
        var show = newShow();

        show.open();

        assertTrue(show.isOpen());
    }

    @Test
    void open_whenCancelled_throwsException() {
        var show = newShow();
        show.cancel("Venue maintenance");

        assertThrows(IllegalStateException.class, show::open);
    }

    @Test
    void domainEventRegistered_whenOpened() {
        var show = newShow();

        show.open();

        var events = show.pullDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(ShowOpened.class, events.getFirst());
    }

    @Test
    void book_whenOpen_withAvailableSeats_marksSeatsAsBooked() {
        var show = newShow();
        show.open();
        var seats = List.of(showSeat(show), showSeat(show));

        show.book(seats);

        seats.forEach(s -> assertEquals(ShowSeatStatus.BOOKED, s.status()));
    }

    @Test
    void book_whenNotOpen_throwsShowNotBookableException() {
        var show = newShow();
        var seats = List.of(showSeat(show));

        assertThrows(ShowNotBookableException.class, () -> show.book(seats));
    }

    @Test
    void book_whenCancelled_throwsShowNotBookableException() {
        var show = newShow();
        show.cancel("Venue maintenance");
        var seats = List.of(showSeat(show));

        var ex = assertThrows(ShowNotBookableException.class, () -> show.book(seats));
        assertTrue(ex.reason().contains("CANCELLED"));
    }

    @Test
    void book_withUnavailableSeat_throwsSeatUnavailableException() {
        var show = newShow();
        show.open();
        var seat = showSeat(show);
        seat.book(); // already booked
        var seats = List.of(showSeat(show), seat);

        assertThrows(SeatUnavailableException.class, () -> show.book(seats));
    }

    private ShowSeat showSeat(Show show) {
        return ShowSeat.create(show.id(), SeatId.generate(), new SeatRow("A"), new SeatNumber(1));
    }

    private Show newShow() {
        return Show.schedule(
            VenueId.generate(),
            "Hamlet",
            new ShowSchedule(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)),
            Money.of(new BigDecimal("42.50"), "EUR"));
    }
}
