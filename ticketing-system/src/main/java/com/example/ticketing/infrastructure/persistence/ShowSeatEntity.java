package com.example.ticketing.infrastructure.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "show_seats")
public class ShowSeatEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "show_id", columnDefinition = "uuid", nullable = false)
    public UUID showId;

    @Column(name = "seat_id", columnDefinition = "uuid", nullable = false)
    public UUID seatId;

    @Column(name = "row_label", nullable = false)
    public String rowLabel;

    @Column(name = "seat_number", nullable = false)
    public int seatNumber;

    @Column(nullable = false)
    public String status;

    @Version
    public int version;

    public static Uni<List<ShowSeatEntity>> findByShowId(UUID showId) {
        return find("showId", showId).list();
    }

    public static Uni<Optional<ShowSeatEntity>> findByShowAndSeat(UUID showId, UUID seatId) {
        return find("showId = ?1 and seatId = ?2", showId, seatId).firstResult()
            .map(entity -> Optional.ofNullable((ShowSeatEntity) entity));
    }

    public static Uni<List<ShowSeatEntity>> findByShowAndSeatIds(UUID showId, List<UUID> seatIds) {
        return find("showId = ?1 and seatId in ?2", showId, seatIds).list();
    }

    public static Uni<List<ShowSeatEntity>> findByIds(List<UUID> ids) {
        return find("id in ?1", ids).list();
    }
}
