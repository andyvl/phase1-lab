package com.example.ticketing.infrastructure.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "seats")
public class SeatEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "venue_id", columnDefinition = "uuid", nullable = false)
    public UUID venueId;

    @Column(name = "row_label", nullable = false)
    public String rowLabel;

    @Column(name = "seat_number", nullable = false)
    public int seatNumber;

    public static Uni<List<SeatEntity>> findByVenueId(UUID venueId) {
        return find("venueId", venueId).list();
    }
}
