package com.example.ticketing.application.booking;

import com.example.ticketing.domain.booking.Booking;
import com.example.ticketing.domain.booking.BookingLine;
import com.example.ticketing.domain.booking.BookingResult;
import com.example.ticketing.domain.booking.BookingStatus;
import com.example.ticketing.domain.booking.Customer;
import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.ShowId;
import com.example.ticketing.domain.show.ShowSeat;
import com.example.ticketing.domain.show.ShowSeatId;
import com.example.ticketing.domain.show.ShowSeatStatus;
import com.example.ticketing.domain.venue.SeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;
import com.example.ticketing.infrastructure.persistence.BookingEntity;
import com.example.ticketing.infrastructure.persistence.BookingLineEntity;
import com.example.ticketing.infrastructure.persistence.CustomerEntity;
import com.example.ticketing.infrastructure.persistence.ShowEntity;
import com.example.ticketing.infrastructure.persistence.ShowSeatEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    @WithTransaction
    public Uni<BookingResult> bookSeats(BookSeatsCommand cmd) {
        validateSeatIds(cmd.seatIds());
        return ShowEntity.<ShowEntity>findById(cmd.showId())
            .flatMap(show -> {
                if (show == null) {
                    return Uni.createFrom().item(new BookingResult.ShowNotFound(new ShowId(cmd.showId())));
                }
                if (!"OPEN".equals(show.status)) {
                    return Uni.createFrom().item(
                        new BookingResult.ShowNotBookable(new ShowId(show.id), "Show is currently " + show.status));
                }
                return ShowSeatEntity.findByShowAndSeatIds(cmd.showId(), cmd.seatIds())
                    .flatMap(showSeats -> attemptBooking(cmd, show, showSeats));
            });
    }

    @WithSession
    public Uni<BookingView> getBooking(UUID id) {
        return BookingEntity.<BookingEntity>findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Booking %s not found".formatted(id)))
            .flatMap(booking -> BookingLineEntity.findByBookingId(id)
                .flatMap(lines -> CustomerEntity.<CustomerEntity>findById(booking.customerId)
                    .flatMap(customer -> ShowEntity.<ShowEntity>findById(booking.showId)
                        .map(show -> new BookingView(
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
                            booking.createdAt)))));
    }

    @WithTransaction
    public Uni<BookingResult> cancelBooking(UUID bookingId) {
        return BookingEntity.<BookingEntity>findById(bookingId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Booking %s not found".formatted(bookingId)))
            .flatMap(booking -> {
                booking.status = BookingStatus.CANCELLED.name();
                return BookingLineEntity.findByBookingId(bookingId)
                    .flatMap(lines -> {
                        var showSeatIds = lines.stream().map(l -> l.showSeatId).toList();
                        if (showSeatIds.isEmpty()) {
                            return Uni.createFrom().item(
                                new BookingResult.Success(booking.id, List.of(), booking.totalAmount, booking.totalCurrency));
                        }
                        return ShowSeatEntity.findByIds(showSeatIds)
                            .map(showSeats -> {
                                showSeats.forEach(seat -> seat.status = ShowSeatStatus.AVAILABLE.name());
                                return (BookingResult) new BookingResult.Success(booking.id,
                                    showSeats.stream().map(s -> s.seatId).toList(),
                                    booking.totalAmount, booking.totalCurrency);
                            });
                    });
            });
    }

    private Uni<BookingResult> attemptBooking(BookSeatsCommand cmd, ShowEntity show, List<ShowSeatEntity> showSeats) {
        var requestedIds = Set.copyOf(cmd.seatIds());
        if (showSeats.size() != requestedIds.size()) {
            var foundIds = showSeats.stream().map(s -> s.seatId).collect(Collectors.toSet());
            var missing = requestedIds.stream().filter(id -> !foundIds.contains(id))
                .findFirst().orElse(cmd.seatIds().getFirst());
            return Uni.createFrom().item(new BookingResult.SeatUnavailable(new ShowId(cmd.showId()), missing));
        }
        for (var seat : showSeats) {
            if (!ShowSeatStatus.AVAILABLE.name().equals(seat.status)) {
                return Uni.createFrom().item(new BookingResult.SeatUnavailable(new ShowId(cmd.showId()), seat.seatId));
            }
        }

        var customer = new Customer(cmd.customerName(), cmd.customerEmail());
        var booking = Booking.create(
            new ShowId(show.id), customer,
            showSeats.stream().map(this::toDomainSeat).toList(),
            new Money(show.ticketPriceAmount, show.ticketPriceCurrency));

        var customerEntity = new CustomerEntity();
        customerEntity.id = booking.customerId().value();
        customerEntity.name = customer.name();
        customerEntity.email = customer.email();
        customerEntity.createdAt = Instant.now();

        var bookingEntity = new BookingEntity();
        bookingEntity.id = booking.id().value();
        bookingEntity.customerId = booking.customerId().value();
        bookingEntity.showId = booking.showId().value();
        bookingEntity.status = booking.status().name();
        bookingEntity.totalAmount = booking.totalAmount().amount();
        bookingEntity.totalCurrency = booking.totalAmount().currency();
        bookingEntity.createdAt = booking.createdAt();

        var lineEntities = booking.lines().stream().map(this::toLineEntity).toList();
        showSeats.forEach(s -> s.status = ShowSeatStatus.BOOKED.name());

        return customerEntity.<CustomerEntity>persist()
            .chain(() -> bookingEntity.persist())
            .chain(() -> persistAll(lineEntities))
            .map(v -> (BookingResult) new BookingResult.Success(
                booking.id().value(), cmd.seatIds(),
                booking.totalAmount().amount(), booking.totalAmount().currency()));
    }

    private Uni<Void> persistAll(List<? extends PanacheEntityBase> entities) {
        var chain = Uni.createFrom().voidItem();
        for (var entity : entities) {
            chain = chain.chain(() -> entity.persist().replaceWithVoid());
        }
        return chain;
    }

    private ShowSeat toDomainSeat(ShowSeatEntity e) {
        return ShowSeat.restore(
            new ShowSeatId(e.id), new ShowId(e.showId), new SeatId(e.seatId),
            new SeatRow(e.rowLabel), new SeatNumber(e.seatNumber),
            ShowSeatStatus.valueOf(e.status), e.version);
    }

    private BookingLineEntity toLineEntity(BookingLine line) {
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

    private void validateSeatIds(List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty())
            throw new IllegalArgumentException("At least one seat must be requested");
        if (new HashSet<>(seatIds).size() != seatIds.size())
            throw new IllegalArgumentException("Seat ids must be unique within a booking request");
    }
}
