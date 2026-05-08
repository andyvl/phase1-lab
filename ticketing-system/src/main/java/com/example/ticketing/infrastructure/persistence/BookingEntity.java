package com.example.ticketing.infrastructure.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
public class BookingEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "customer_id", columnDefinition = "uuid", nullable = false)
    public UUID customerId;

    @Column(name = "show_id", columnDefinition = "uuid", nullable = false)
    public UUID showId;

    @Column(nullable = false)
    public String status;

    @Column(name = "total_amount", nullable = false)
    public BigDecimal totalAmount;

    @Column(name = "total_currency", nullable = false)
    public String totalCurrency;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static Uni<BookingEntity> findByIdReactive(UUID id) {
        return findById(id);
    }
}
