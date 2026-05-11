package com.example.ticketing.domain.booking;

import com.example.ticketing.domain.shared.AggregateRoot;
import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.ShowId;
import com.example.ticketing.domain.show.ShowSeat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class Booking extends AggregateRoot {
    private BookingId id;
    private CustomerId customerId;
    private Customer customer;
    private ShowId showId;
    private BookingStatus status;
    private List<BookingLine> lines;
    private Instant createdAt;

    private Booking() {
    }

    public static Booking create(ShowId showId, Customer customer, List<ShowSeat> seats, Money pricePerSeat) {
        Objects.requireNonNull(showId, "showId is required");
        Objects.requireNonNull(customer, "customer is required");
        Objects.requireNonNull(seats, "seats are required");
        Objects.requireNonNull(pricePerSeat, "pricePerSeat is required");
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }
        var booking = new Booking();
        booking.id = BookingId.generate();
        booking.customerId = CustomerId.generate();
        booking.customer = customer;
        booking.showId = showId;
        booking.status = BookingStatus.CONFIRMED;
        booking.createdAt = Instant.now();
        booking.lines = seats.stream()
            .map(seat -> BookingLine.create(booking.id, seat, pricePerSeat))
            .toList();
        booking.registerEvent(BookingCreated.of(booking.id, booking.customerId));
        return booking;
    }

    public static Booking restore(BookingId id, CustomerId customerId, Customer customer, ShowId showId, BookingStatus status,
                                  List<BookingLine> lines, Instant createdAt) {
        var booking = new Booking();
        booking.id = id;
        booking.customerId = customerId;
        booking.customer = customer;
        booking.showId = showId;
        booking.status = status;
        booking.lines = List.copyOf(lines);
        booking.createdAt = createdAt;
        return booking;
    }

    public void cancel() {
        if (status == BookingStatus.CANCELLED) {
            return;
        }
        this.status = BookingStatus.CANCELLED;
        registerEvent(BookingCancelled.of(this.id, this.customerId));
    }

    public Money totalAmount() {
        return lines.stream()
            .map(BookingLine::price)
            .reduce(new Money(BigDecimal.ZERO, lines.getFirst().price().currency()), Money::add);
    }

    public BookingId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Customer customer() {
        return customer;
    }

    public ShowId showId() {
        return showId;
    }

    public BookingStatus status() {
        return status;
    }

    public List<BookingLine> lines() {
        return List.copyOf(lines);
    }

    public Instant createdAt() {
        return createdAt;
    }
}
