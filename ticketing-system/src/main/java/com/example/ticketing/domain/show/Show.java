package com.example.ticketing.domain.show;

import com.example.ticketing.domain.shared.AggregateRoot;
import com.example.ticketing.domain.shared.Money;
import com.example.ticketing.domain.venue.VenueId;
import java.util.Objects;

public final class Show extends AggregateRoot {
    private ShowId id;
    private VenueId venueId;
    private String title;
    private ShowSchedule schedule;
    private Money ticketPrice;
    private ShowStatus status;

    private Show() {
    }

    public static Show schedule(VenueId venueId, String title, ShowSchedule schedule, Money ticketPrice) {
        Objects.requireNonNull(venueId, "venueId is required");
        Objects.requireNonNull(title, "title is required");
        Objects.requireNonNull(schedule, "schedule is required");
        Objects.requireNonNull(ticketPrice, "ticketPrice is required");
        if (title.isBlank()) {
            throw new IllegalArgumentException("Show title cannot be blank");
        }
        var show = new Show();
        show.id = ShowId.generate();
        show.venueId = venueId;
        show.title = title;
        show.schedule = schedule;
        show.ticketPrice = ticketPrice;
        show.status = new ShowStatus.Scheduled();
        return show;
    }

    public static Show restore(ShowId id, VenueId venueId, String title, ShowSchedule schedule, Money ticketPrice, ShowStatus status) {
        var show = new Show();
        show.id = Objects.requireNonNull(id, "id is required");
        show.venueId = Objects.requireNonNull(venueId, "venueId is required");
        show.title = Objects.requireNonNull(title, "title is required");
        show.schedule = Objects.requireNonNull(schedule, "schedule is required");
        show.ticketPrice = Objects.requireNonNull(ticketPrice, "ticketPrice is required");
        show.status = Objects.requireNonNull(status, "status is required");
        return show;
    }

    public void open() {
        if (!(status instanceof ShowStatus.Scheduled)) {
            throw new IllegalStateException("Can only open a scheduled show");
        }
        this.status = new ShowStatus.Open();
        registerEvent(ShowOpened.of(this.id));
    }

    public void cancel(String reason) {
        Objects.requireNonNull(reason, "reason is required");
        if (status instanceof ShowStatus.Cancelled) {
            return;
        }
        this.status = new ShowStatus.Cancelled(reason);
    }

    public boolean isOpen() {
        return status instanceof ShowStatus.Open;
    }

    public ShowId id() {
        return id;
    }

    public VenueId venueId() {
        return venueId;
    }

    public String title() {
        return title;
    }

    public ShowSchedule schedule() {
        return schedule;
    }

    public Money ticketPrice() {
        return ticketPrice;
    }

    public ShowStatus status() {
        return status;
    }
}
