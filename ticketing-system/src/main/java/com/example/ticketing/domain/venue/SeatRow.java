package com.example.ticketing.domain.venue;

import java.util.Objects;

public record SeatRow(String label) {
    public SeatRow {
        Objects.requireNonNull(label, "label is required");
        if (label.isBlank()) {
            throw new IllegalArgumentException("Row label cannot be blank");
        }
    }
}
