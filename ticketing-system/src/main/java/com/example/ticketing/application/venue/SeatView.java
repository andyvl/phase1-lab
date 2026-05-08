package com.example.ticketing.application.venue;

import java.util.UUID;

public record SeatView(UUID id, String row, int number) {
}
