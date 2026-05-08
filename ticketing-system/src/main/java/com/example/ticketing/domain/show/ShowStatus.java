package com.example.ticketing.domain.show;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ShowStatus.Scheduled.class, name = "SCHEDULED"),
    @JsonSubTypes.Type(value = ShowStatus.Open.class, name = "OPEN"),
    @JsonSubTypes.Type(value = ShowStatus.SoldOut.class, name = "SOLD_OUT"),
    @JsonSubTypes.Type(value = ShowStatus.Cancelled.class, name = "CANCELLED")
})
public sealed interface ShowStatus permits ShowStatus.Scheduled, ShowStatus.Open, ShowStatus.SoldOut, ShowStatus.Cancelled {
    record Scheduled() implements ShowStatus {
    }

    record Open() implements ShowStatus {
    }

    record SoldOut() implements ShowStatus {
    }

    record Cancelled(String reason) implements ShowStatus {
    }
}
