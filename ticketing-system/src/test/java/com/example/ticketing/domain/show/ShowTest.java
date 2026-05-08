package com.example.ticketing.domain.show;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.venue.VenueId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private Show newShow() {
        return Show.schedule(
            VenueId.generate(),
            "Hamlet",
            new ShowSchedule(LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2)),
            Money.of(new BigDecimal("42.50"), "EUR"));
    }
}
