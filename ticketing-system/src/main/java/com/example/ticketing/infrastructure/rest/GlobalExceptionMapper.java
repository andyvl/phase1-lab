package com.example.ticketing.infrastructure.rest;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.concurrent.CompletionException;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        var cause = unwrap(exception);
        if (cause instanceof NotFoundException notFound) {
            return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ProblemDetail("Not found", notFound.getMessage()))
                .build();
        }
        if (cause instanceof IllegalArgumentException illegalArgument) {
            return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ProblemDetail("Invalid request", illegalArgument.getMessage()))
                .build();
        }
        if (cause instanceof OptimisticLockException) {
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ProblemDetail("Conflict", "Seat was taken concurrently, please retry"))
                .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(new ProblemDetail("Internal server error", cause.getMessage() == null ? "Unexpected error" : cause.getMessage()))
            .build();
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
