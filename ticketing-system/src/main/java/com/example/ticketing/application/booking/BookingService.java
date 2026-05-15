package com.example.ticketing.application.booking;

import com.example.ticketing.domain.booking.Booking;
import com.example.ticketing.domain.booking.BookingResult;
import com.example.ticketing.domain.booking.BookingStatus;
import com.example.ticketing.domain.booking.Customer;
import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.SeatUnavailableException;
import com.example.ticketing.domain.show.ShowId;
import com.example.ticketing.domain.show.ShowNotBookableException;
import com.example.ticketing.domain.show.ShowSeatStatus;
import com.example.ticketing.infrastructure.persistence.BookingEntity;
import com.example.ticketing.infrastructure.persistence.BookingLineEntity;
import com.example.ticketing.infrastructure.persistence.BookingPersistenceMapper;
import com.example.ticketing.infrastructure.persistence.CustomerEntity;
import com.example.ticketing.infrastructure.persistence.ShowEntity;
import com.example.ticketing.infrastructure.persistence.ShowSeatEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class BookingService {

    private final BookingPersistenceMapper persistenceMapper = new BookingPersistenceMapper();
    private final BookingViewMapper viewMapper = new BookingViewMapper();

    @WithTransaction
    public Uni<BookingResult> bookSeats(BookSeatsCommand cmd) {
        validateSeatIds(cmd.seatIds());
        return ShowEntity.<ShowEntity>findById(cmd.showId())
            .flatMap(show -> {
                if (show == null) {
                    return Uni.createFrom().item(new BookingResult.ShowNotFound(new ShowId(cmd.showId())));
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
                        .map(show -> viewMapper.toBookingView(booking, customer, show, lines)))));
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

        var domainShow = persistenceMapper.toShowDomain(show);
        var domainSeats = showSeats.stream().map(persistenceMapper::toDomainSeat).toList();
        try {
            domainShow.book(domainSeats);
        } catch (ShowNotBookableException e) {
            return Uni.createFrom().item(new BookingResult.ShowNotBookable(e.showId(), e.reason()));
        } catch (SeatUnavailableException e) {
            return Uni.createFrom().item(new BookingResult.SeatUnavailable(e.showId(), e.seatId()));
        }

        var customer = new Customer(cmd.customerName(), cmd.customerEmail());
        var booking = Booking.create(
            new ShowId(show.id), customer, domainSeats,
            new Money(show.ticketPriceAmount, show.ticketPriceCurrency));

        var customerEntity = persistenceMapper.toCustomerEntity(customer, booking.customerId().value());
        var bookingEntity = persistenceMapper.toBookingEntity(booking);
        var lineEntities = booking.lines().stream().map(persistenceMapper::toLineEntity).toList();
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

    private void validateSeatIds(List<UUID> seatIds) {
        if (seatIds == null || seatIds.isEmpty())
            throw new IllegalArgumentException("At least one seat must be requested");
        if (new HashSet<>(seatIds).size() != seatIds.size())
            throw new IllegalArgumentException("Seat ids must be unique within a booking request");
    }

    /**
     * Maps persistence entities to API view/DTO objects.
     */
    static class BookingViewMapper {

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
}
