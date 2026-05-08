package com.example.ticketing.infrastructure.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shows")
public class ShowEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "venue_id", columnDefinition = "uuid", nullable = false)
    public UUID venueId;

    @Column(nullable = false)
    public String title;

    @Column(name = "starts_at", nullable = false)
    public LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    public LocalDateTime endsAt;

    @Column(name = "ticket_price_amount", nullable = false)
    public BigDecimal ticketPriceAmount;

    @Column(name = "ticket_price_currency", nullable = false)
    public String ticketPriceCurrency;

    @Column(nullable = false)
    public String status;

    @Column(name = "cancel_reason", length = 500)
    public String cancelReason;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static Uni<ShowEntity> findByIdReactive(UUID id) {
        return findById(id);
    }
}
