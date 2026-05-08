package com.example.ticketing.application.venue;

import java.util.List;
import java.util.UUID;

public record VenueView(UUID id, String name, String address, List<SeatView> seats) {
}
