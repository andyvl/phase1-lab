package com.example.ticketing.infrastructure.rest;

import com.example.ticketing.application.RequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;

/**
 * Extracts (or generates) a correlation ID from the request and stores it
 * in the Vert.x routing context so it is accessible throughout the reactive pipeline.
 *
 * Note on ScopedValue: RequestContext.CORRELATION_ID is declared as a ScopedValue<String>
 * (Java 25 JEP 487). ScopedValues bind to a specific thread's execution scope.
 * In a virtual-thread context you'd use:
 *   ScopedValue.runWhere(RequestContext.CORRELATION_ID, id, () -> handle(req));
 * In this reactive (Vert.x event-loop) pipeline we store it as a routing-context
 * attribute so it propagates across async hops via the Vert.x context.
 */
@Provider
public class CorrelationIdFilter implements ContainerRequestFilter {

    static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final String CORRELATION_ATTR   = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        var id = requestContext.getHeaderString(CORRELATION_HEADER);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        // Store in the JAX-RS property bag (accessible from resources and services via injection of request context)
        requestContext.setProperty(CORRELATION_ATTR, id);
    }
}
