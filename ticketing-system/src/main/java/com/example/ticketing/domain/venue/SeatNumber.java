package com.example.ticketing.domain.venue;

public record SeatNumber(int number) {
    public SeatNumber {
        if (number < 1) {
            throw new IllegalArgumentException("Seat number must be positive");
        }
    }
}
