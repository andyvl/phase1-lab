package com.example.ticketing.infrastructure.rest;

import com.example.ticketing.application.venue.AddSeatsCommand;
import com.example.ticketing.application.venue.CreateVenueCommand;
import com.example.ticketing.application.venue.VenueService;
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
import java.util.List;
import java.util.UUID;

@Path("/venues")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VenueResource {

    @Inject
    VenueService venueService;

    @POST
    public Uni<Response> createVenue(CreateVenueRequest req) {
        return venueService.createVenue(new CreateVenueCommand(req.name(), req.address()))
            .map(view -> Response.status(Response.Status.CREATED).entity(view).build());
    }

    @GET
    @Path("/{id}")
    public Uni<Response> getVenue(@PathParam("id") UUID id) {
        return venueService.getVenue(id)
            .map(view -> Response.ok(view).build());
    }

    @POST
    @Path("/{id}/seats")
    public Uni<Response> addSeats(@PathParam("id") UUID id, AddSeatsRequest req) {
        return venueService.addSeats(new AddSeatsCommand(id, req.rowLabel(), req.seatNumbers()))
            .map(view -> Response.ok(view).build());
    }

    public record CreateVenueRequest(String name, String address) {}
    public record AddSeatsRequest(String rowLabel, List<Integer> seatNumbers) {}
}
