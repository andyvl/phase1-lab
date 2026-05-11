package com.example.ticketing.domain.shared;

import java.util.ArrayList;
import java.util.List;

public abstract class AggregateRoot {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {

        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
