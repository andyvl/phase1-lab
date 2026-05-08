package com.example.ticketing.domain.show;

import java.time.LocalDateTime;
import java.util.Objects;

public record ShowSchedule(LocalDateTime startsAt, LocalDateTime endsAt) {
    public ShowSchedule {
        Objects.requireNonNull(startsAt, "startsAt is required");
        Objects.requireNonNull(endsAt, "endsAt is required");
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Show must end after it starts");
        }
    }
}
