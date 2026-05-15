package com.example.ticketing.domain.show;

public class ShowNotBookableException extends RuntimeException {
    private final ShowId showId;

    public ShowNotBookableException(ShowId showId, String reason) {
        super(reason);
        this.showId = showId;
    }

    public ShowId showId() {
        return showId;
    }

    public String reason() {
        return getMessage();
    }
}
