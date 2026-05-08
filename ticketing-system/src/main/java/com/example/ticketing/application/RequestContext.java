package com.example.ticketing.application;

public final class RequestContext {
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    private RequestContext() {
    }

    public static String correlationId() {
        return CORRELATION_ID.orElse("unknown");
    }
}
