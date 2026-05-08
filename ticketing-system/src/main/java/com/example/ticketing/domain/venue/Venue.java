package com.example.ticketing.domain.venue;

import com.example.ticketing.domain.shared.AggregateRoot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Venue extends AggregateRoot {
    private VenueId id;
    private String name;
    private String address;
    private final List<Seat> seats = new ArrayList<>();

    private Venue() {
    }

    public static Venue create(String name, String address) {
        validate(name, address);
        var venue = new Venue();
        venue.id = VenueId.generate();
        venue.name = name;
        venue.address = address;
        return venue;
    }

    public static Venue restore(VenueId id, String name, String address, List<Seat> seats) {
        validate(name, address);
        var venue = new Venue();
        venue.id = Objects.requireNonNull(id, "id is required");
        venue.name = name;
        venue.address = address;
        venue.seats.addAll(Objects.requireNonNullElse(seats, List.of()));
        return venue;
    }

    private static void validate(String name, String address) {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(address, "address is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Venue name cannot be blank");
        }
        if (address.isBlank()) {
            throw new IllegalArgumentException("Venue address cannot be blank");
        }
    }

    public Seat addSeat(SeatRow row, SeatNumber number) {
        boolean duplicate = seats.stream()
            .anyMatch(seat -> seat.row().equals(row) && seat.number().equals(number));
        if (duplicate) {
            throw new IllegalArgumentException(
                "Seat %s%d already exists".formatted(row.label(), number.number()));
        }
        var seat = Seat.create(this.id, row, number);
        seats.add(seat);
        return seat;
    }

    public VenueId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }

    public List<Seat> seats() {
        return List.copyOf(seats);
    }
}
