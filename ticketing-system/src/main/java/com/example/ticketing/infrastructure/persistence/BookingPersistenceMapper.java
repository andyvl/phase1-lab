package com.example.ticketing.infrastructure.persistence;

import com.example.ticketing.domain.booking.Booking;
import com.example.ticketing.domain.booking.BookingLine;
import com.example.ticketing.domain.booking.Customer;
import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.Show;
import com.example.ticketing.domain.show.ShowId;
import com.example.ticketing.domain.show.ShowSchedule;
import com.example.ticketing.domain.show.ShowSeat;
import com.example.ticketing.domain.show.ShowSeatId;
import com.example.ticketing.domain.show.ShowSeatStatus;
import com.example.ticketing.domain.show.ShowStatus;
import com.example.ticketing.domain.venue.SeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;
import com.example.ticketing.domain.venue.VenueId;
import java.time.Instant;
import java.util.UUID;

public class BookingPersistenceMapper {

    public Show toShowDomain(ShowEntity e) {
        return Show.restore(
            new ShowId(e.id), new VenueId(e.venueId), e.title,
            new ShowSchedule(e.startsAt, e.endsAt),
            Money.of(e.ticketPriceAmount, e.ticketPriceCurrency),
            showStatusFromString(e.status, e.cancelReason));
    }

    private ShowStatus showStatusFromString(String status, String cancelReason) {
        return switch (status) {
            case "SCHEDULED" -> new ShowStatus.Scheduled();
            case "OPEN" -> new ShowStatus.Open();
            case "SOLD_OUT" -> new ShowStatus.SoldOut();
            case "CANCELLED" -> new ShowStatus.Cancelled(cancelReason != null ? cancelReason : "cancelled");
            default -> throw new IllegalArgumentException("Unknown show status: " + status);
        };
    }

    public ShowSeat toDomainSeat(ShowSeatEntity e) {
        return ShowSeat.restore(
            new ShowSeatId(e.id), new ShowId(e.showId), new SeatId(e.seatId),
            new SeatRow(e.rowLabel), new SeatNumber(e.seatNumber),
            ShowSeatStatus.valueOf(e.status), e.version);
    }

    public CustomerEntity toCustomerEntity(Customer customer, UUID id) {
        var e = new CustomerEntity();
        e.id = id;
        e.name = customer.name();
        e.email = customer.email();
        e.createdAt = Instant.now();
        return e;
    }

    public BookingEntity toBookingEntity(Booking booking) {
        var e = new BookingEntity();
        e.id = booking.id().value();
        e.customerId = booking.customerId().value();
        e.showId = booking.showId().value();
        e.status = booking.status().name();
        e.totalAmount = booking.totalAmount().amount();
        e.totalCurrency = booking.totalAmount().currency();
        e.createdAt = booking.createdAt();
        return e;
    }

    public BookingLineEntity toLineEntity(BookingLine line) {
        var e = new BookingLineEntity();
        e.id = line.id().value();
        e.bookingId = line.bookingId().value();
        e.showSeatId = line.showSeatId().value();
        e.rowLabel = line.row().label();
        e.seatNumber = line.number().number();
        e.priceAmount = line.price().amount();
        e.priceCurrency = line.price().currency();
        return e;
    }
}
