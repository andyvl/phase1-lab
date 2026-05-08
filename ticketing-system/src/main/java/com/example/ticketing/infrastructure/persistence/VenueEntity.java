package com.example.ticketing.infrastructure.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "venues")
public class VenueEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, length = 500)
    public String address;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static Uni<VenueEntity> findByIdReactive(UUID id) {
        return findById(id);
    }

    public static Uni<List<VenueEntity>> listAllReactive() {
        return listAll();
    }
}
