package com.example.ticketing.application.show;

import java.util.UUID;

public record ShowSeatView(UUID id, UUID seatId, String row, int number, String status) {
}
