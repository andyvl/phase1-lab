package com.example.ticketing.application.show;

import com.example.ticketing.domain.show.ShowStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ShowView(UUID id, String title, String venueName, LocalDateTime startsAt, ShowStatus status,
                       List<ShowSeatView> seats) {
}
