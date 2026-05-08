package com.example.ticketing.application.show;

import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.show.Show;
import com.example.ticketing.domain.show.ShowId;
import com.example.ticketing.domain.show.ShowSchedule;
import com.example.ticketing.domain.show.ShowSeat;
import com.example.ticketing.domain.show.ShowStatus;
import com.example.ticketing.domain.venue.SeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;
import com.example.ticketing.domain.venue.VenueId;
import com.example.ticketing.infrastructure.persistence.SeatEntity;
import com.example.ticketing.infrastructure.persistence.ShowEntity;
import com.example.ticketing.infrastructure.persistence.ShowSeatEntity;
import com.example.ticketing.infrastructure.persistence.VenueEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ShowService {

    @WithTransaction
    public Uni<ShowView> scheduleShow(ScheduleShowCommand cmd) {
        return VenueEntity.<VenueEntity>findById(cmd.venueId())
            .onItem().ifNull().failWith(() -> new NotFoundException("Venue %s not found".formatted(cmd.venueId())))
            .flatMap(venueEntity -> SeatEntity.findByVenueId(cmd.venueId())
                .flatMap(venueSeats -> {
                    var show = Show.schedule(
                        new VenueId(cmd.venueId()),
                        cmd.title(),
                        new ShowSchedule(cmd.startsAt(), cmd.endsAt()),
                        Money.of(cmd.ticketPriceAmount(), cmd.ticketPriceCurrency()));

                    var showEntity = new ShowEntity();
                    showEntity.id = show.id().value();
                    showEntity.venueId = show.venueId().value();
                    showEntity.title = show.title();
                    showEntity.startsAt = show.schedule().startsAt();
                    showEntity.endsAt = show.schedule().endsAt();
                    showEntity.ticketPriceAmount = show.ticketPrice().amount();
                    showEntity.ticketPriceCurrency = show.ticketPrice().currency();
                    showEntity.status = statusToString(show.status());
                    showEntity.createdAt = Instant.now();

                    var showSeats = venueSeats.stream().map(seat -> {
                        var domainSeat = ShowSeat.create(show.id(), new SeatId(seat.id),
                            new SeatRow(seat.rowLabel), new SeatNumber(seat.seatNumber));
                        var e = new ShowSeatEntity();
                        e.id = domainSeat.id().value();
                        e.showId = show.id().value();
                        e.seatId = seat.id;
                        e.rowLabel = seat.rowLabel;
                        e.seatNumber = seat.seatNumber;
                        e.status = domainSeat.status().name();
                        return e;
                    }).toList();

                    return showEntity.<ShowEntity>persist()
                        .chain(() -> persistAll(showSeats))
                        .map(v -> toView(showEntity, venueEntity.name, showSeats));
                }));
    }

    @WithSession
    public Uni<ShowView> getShow(UUID id) {
        return ShowEntity.<ShowEntity>findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Show %s not found".formatted(id)))
            .flatMap(show -> VenueEntity.<VenueEntity>findById(show.venueId)
                .flatMap(venue -> ShowSeatEntity.findByShowId(id)
                    .map(showSeats -> toView(show, venue != null ? venue.name : "Unknown", showSeats))));
    }

    @WithTransaction
    public Uni<ShowView> openShow(UUID showId) {
        return ShowEntity.<ShowEntity>findById(showId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Show %s not found".formatted(showId)))
            .flatMap(entity -> {
                var domain = Show.restore(
                    new ShowId(entity.id), new VenueId(entity.venueId), entity.title,
                    new ShowSchedule(entity.startsAt, entity.endsAt),
                    Money.of(entity.ticketPriceAmount, entity.ticketPriceCurrency),
                    statusFromEntity(entity.status, entity.cancelReason));
                domain.open();
                entity.status = statusToString(domain.status());
                entity.cancelReason = null;
                return VenueEntity.<VenueEntity>findById(entity.venueId)
                    .flatMap(venue -> ShowSeatEntity.findByShowId(showId)
                        .map(showSeats -> toView(entity, venue != null ? venue.name : "Unknown", showSeats)));
            });
    }

    private Uni<Void> persistAll(List<? extends PanacheEntityBase> entities) {
        var chain = Uni.createFrom().voidItem();
        for (var entity : entities) {
            chain = chain.chain(() -> entity.persist().replaceWithVoid());
        }
        return chain;
    }

    private ShowView toView(ShowEntity show, String venueName, List<ShowSeatEntity> showSeats) {
        return new ShowView(
            show.id, show.title, venueName, show.startsAt,
            statusFromEntity(show.status, show.cancelReason),
            showSeats.stream()
                .sorted(Comparator.comparing((ShowSeatEntity s) -> s.rowLabel).thenComparingInt(s -> s.seatNumber))
                .map(s -> new ShowSeatView(s.id, s.seatId, s.rowLabel, s.seatNumber, s.status))
                .toList());
    }

    public String statusToString(ShowStatus status) {
        return switch (status) {
            case ShowStatus.Scheduled ignored -> "SCHEDULED";
            case ShowStatus.Open ignored -> "OPEN";
            case ShowStatus.SoldOut ignored -> "SOLD_OUT";
            case ShowStatus.Cancelled ignored -> "CANCELLED";
        };
    }

    public ShowStatus statusFromEntity(String status, String cancelReason) {
        return switch (status) {
            case "SCHEDULED" -> new ShowStatus.Scheduled();
            case "OPEN" -> new ShowStatus.Open();
            case "SOLD_OUT" -> new ShowStatus.SoldOut();
            case "CANCELLED" -> new ShowStatus.Cancelled(cancelReason == null ? "cancelled" : cancelReason);
            default -> throw new IllegalArgumentException("Unknown show status: " + status);
        };
    }
}
