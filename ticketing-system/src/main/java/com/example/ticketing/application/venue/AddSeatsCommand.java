package com.example.ticketing.application.venue;

import java.util.List;
import java.util.UUID;

public record AddSeatsCommand(UUID venueId, String rowLabel, List<Integer> seatNumbers) {
}
