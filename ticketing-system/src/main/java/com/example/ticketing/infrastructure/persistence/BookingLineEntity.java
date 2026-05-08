package com.example.ticketing.infrastructure.persistence;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "booking_lines")
public class BookingLineEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;

    @Column(name = "booking_id", columnDefinition = "uuid", nullable = false)
    public UUID bookingId;

    @Column(name = "show_seat_id", columnDefinition = "uuid", nullable = false)
    public UUID showSeatId;

    @Column(name = "row_label", nullable = false)
    public String rowLabel;

    @Column(name = "seat_number", nullable = false)
    public int seatNumber;

    @Column(name = "price_amount", nullable = false)
    public BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false)
    public String priceCurrency;

    public static Uni<List<BookingLineEntity>> findByBookingId(UUID bookingId) {
        return find("bookingId", bookingId).list();
    }
}
