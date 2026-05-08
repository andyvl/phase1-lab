package com.example.ticketing.infrastructure.rest;

import com.example.ticketing.application.show.ScheduleShowCommand;
import com.example.ticketing.application.show.ShowService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Path("/shows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShowResource {

    @Inject
    ShowService showService;

    @POST
    public Uni<Response> scheduleShow(ScheduleShowRequest req) {
        return showService.scheduleShow(new ScheduleShowCommand(
            req.venueId(), req.title(), req.startsAt(), req.endsAt(),
            req.ticketPriceAmount(), req.ticketPriceCurrency()))
            .map(view -> Response.status(Response.Status.CREATED).entity(view).build());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getShow(@PathParam("id") UUID id) {
        return showService.getShow(id)
            .map(view -> Response.ok(view).build());
    }

    @POST
    @Path("/{id}/open")
    public Uni<Response> openShow(@PathParam("id") UUID id) {
        return showService.openShow(id)
            .map(view -> Response.ok(view).build());
    }

    public record ScheduleShowRequest(
        UUID venueId, String title,
        LocalDateTime startsAt, LocalDateTime endsAt,
        BigDecimal ticketPriceAmount, String ticketPriceCurrency) {}
}
