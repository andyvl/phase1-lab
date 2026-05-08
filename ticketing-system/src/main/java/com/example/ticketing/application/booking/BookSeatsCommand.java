package com.example.ticketing.application.booking;

import java.util.List;
import java.util.UUID;

public record BookSeatsCommand(UUID showId, List<UUID> seatIds, String customerName, String customerEmail) {
}
