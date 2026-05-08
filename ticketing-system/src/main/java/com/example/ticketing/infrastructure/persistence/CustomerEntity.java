package com.example.ticketing.infrastructure.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String email;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static Uni<CustomerEntity> findByIdReactive(UUID id) {
        return findById(id);
    }
}
