package com.example.ticketing.application.venue;

import com.example.ticketing.domain.venue.Seat;
import com.example.ticketing.domain.venue.SeatId;
import com.example.ticketing.domain.venue.SeatNumber;
import com.example.ticketing.domain.venue.SeatRow;
import com.example.ticketing.domain.venue.Venue;
import com.example.ticketing.domain.venue.VenueId;
import com.example.ticketing.infrastructure.persistence.SeatEntity;
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
public class VenueService {

    @WithTransaction
    public Uni<VenueView> createVenue(CreateVenueCommand cmd) {
        var venue = Venue.create(cmd.name(), cmd.address());
        var entity = new VenueEntity();
        entity.id = venue.id().value();
        entity.name = venue.name();
        entity.address = venue.address();
        entity.createdAt = Instant.now();
        return entity.<VenueEntity>persist().map(v -> toView(v, List.of()));
    }

    @WithSession
    public Uni<VenueView> getVenue(UUID id) {
        return VenueEntity.<VenueEntity>findById(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Venue %s not found".formatted(id)))
            .flatMap(venue -> SeatEntity.findByVenueId(id).map(seats -> toView(venue, seats)));
    }

    @WithTransaction
    public Uni<VenueView> addSeats(AddSeatsCommand cmd) {
        return VenueEntity.<VenueEntity>findById(cmd.venueId())
            .onItem().ifNull().failWith(() -> new NotFoundException("Venue %s not found".formatted(cmd.venueId())))
            .flatMap(venueEntity -> SeatEntity.findByVenueId(cmd.venueId())
                .flatMap(existingSeats -> {
                    var venue = Venue.restore(
                        new VenueId(venueEntity.id),
                        venueEntity.name,
                        venueEntity.address,
                        existingSeats.stream().map(this::toDomainSeat).toList());
                    var row = new SeatRow(cmd.rowLabel());
                    var newSeats = cmd.seatNumbers().stream()
                        .map(SeatNumber::new)
                        .map(n -> venue.addSeat(row, n))
                        .map(this::toSeatEntity)
                        .toList();
                    return persistAll(newSeats).map(v ->
                        new VenueView(venueEntity.id, venueEntity.name, venueEntity.address,
                            venue.seats().stream()
                                .sorted(Comparator.comparing((Seat s) -> s.row().label())
                                    .thenComparingInt(s -> s.number().number()))
                                .map(this::toView)
                                .toList()));
                }));
    }

    private Uni<Void> persistAll(List<? extends PanacheEntityBase> entities) {
        var chain = Uni.createFrom().voidItem();
        for (var entity : entities) {
            chain = chain.chain(() -> entity.persist().replaceWithVoid());
        }
        return chain;
    }

    private Seat toDomainSeat(SeatEntity e) {
        return Seat.restore(new SeatId(e.id), new VenueId(e.venueId), new SeatRow(e.rowLabel), new SeatNumber(e.seatNumber));
    }

    private SeatEntity toSeatEntity(Seat seat) {
        var e = new SeatEntity();
        e.id = seat.id().value();
        e.venueId = seat.venueId().value();
        e.rowLabel = seat.row().label();
        e.seatNumber = seat.number().number();
        return e;
    }

    private VenueView toView(VenueEntity entity, List<SeatEntity> seats) {
        return new VenueView(entity.id, entity.name, entity.address,
            seats.stream()
                .sorted(Comparator.comparing((SeatEntity s) -> s.rowLabel).thenComparingInt(s -> s.seatNumber))
                .map(s -> new SeatView(s.id, s.rowLabel, s.seatNumber))
                .toList());
    }

    private SeatView toView(Seat seat) {
        return new SeatView(seat.id().value(), seat.row().label(), seat.number().number());
    }
}
