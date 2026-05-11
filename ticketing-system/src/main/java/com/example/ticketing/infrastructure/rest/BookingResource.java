package com.example.ticketing.infrastructure.rest;

import com.example.ticketing.application.booking.BookSeatsCommand;
import com.example.ticketing.application.booking.BookingService;
import com.example.ticketing.domain.booking.BookingResult;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingResource {

    @Inject
    BookingService bookingService;

    @POST
    public Uni<Response> bookSeats(BookSeatsRequest req) {
        return bookingService.bookSeats(toCommand(req))
            .map(result -> switch (result) {
                case BookingResult.Success s ->
                    Response.status(Response.Status.CREATED).entity(s).build();
                case BookingResult.SeatUnavailable u ->
                    Response.status(Response.Status.CONFLICT)
                        .entity(new ProblemDetail("Seat unavailable",
                            "Seat %s for show %s is not available".formatted(u.seatId(), u.showId().value())))
                        .build();
                case BookingResult.ShowNotFound nf ->
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(new ProblemDetail("Show not found",
                            "Show %s not found".formatted(nf.showId().value())))
                        .build();
                case BookingResult.ShowNotBookable nb ->
                    Response.status(422)
                        .entity(new ProblemDetail("Show not bookable", nb.reason()))
                        .build();
            });
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getBooking(@PathParam("id") UUID id) {
        return bookingService.getBooking(id)
            .map(view -> Response.ok(view).build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> cancelBooking(@PathParam("id") UUID id) {
        return bookingService.cancelBooking(id)
            .map(result -> switch (result) {
                case BookingResult.Success s -> Response.ok(s).build();
                case BookingResult.SeatUnavailable u ->
                    Response.status(Response.Status.CONFLICT)
                        .entity(new ProblemDetail("Seat unavailable",
                            "Seat %s for show %s is not available".formatted(u.seatId(), u.showId().value())))
                        .build();
                case BookingResult.ShowNotFound nf ->
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(new ProblemDetail("Show not found",
                            "Show %s not found".formatted(nf.showId().value())))
                        .build();
                case BookingResult.ShowNotBookable nb ->
                    Response.status(422)
                        .entity(new ProblemDetail("Show not bookable", nb.reason()))
                        .build();
            });
    }

    private BookSeatsCommand toCommand(BookSeatsRequest req) {
        return new BookSeatsCommand(req.showId(), req.seatIds(), req.customerName(), req.customerEmail());
    }

    public record BookSeatsRequest(UUID showId, List<UUID> seatIds, String customerName, String customerEmail) {}
}
