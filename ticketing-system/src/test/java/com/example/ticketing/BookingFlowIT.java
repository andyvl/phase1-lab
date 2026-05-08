package com.example.ticketing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import com.example.ticketing.infrastructure.testing.PostgresProfile;
import com.example.ticketing.infrastructure.testing.PostgreSQLTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(PostgresProfile.class)
@QuarkusTestResource(PostgreSQLTestResource.class)
class BookingFlowIT {
    @Test
    void fullFlow_createVenueAddSeatsScheduleOpenAndBook_returns201() {
        var venueId = createVenue();
        addSeats(venueId, List.of(1, 2));
        var showId = scheduleShow(venueId);
        openShow(showId);
        var seatId = firstSeatId(showId);

        given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "showId", showId,
                "seatIds", List.of(seatId),
                "customerName", "Ada Lovelace",
                "customerEmail", "ada@example.com"))
        .when()
            .post("/bookings")
        .then()
            .statusCode(201)
            .body("bookingId", notNullValue());
    }

    @Test
    void cancelBooking_releasesSeats() {
        var venueId = createVenue();
        addSeats(venueId, List.of(11));
        var showId = scheduleShow(venueId);
        openShow(showId);
        var seatId = firstSeatId(showId);
        var bookingId = given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "showId", showId,
                "seatIds", List.of(seatId),
                "customerName", "Grace Hopper",
                "customerEmail", "grace@example.com"))
        .when()
            .post("/bookings")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getUUID("bookingId");

        given()
        .when()
            .delete("/bookings/{id}", bookingId)
        .then()
            .statusCode(200);

        given()
        .when()
            .get("/shows/{id}", showId)
        .then()
            .statusCode(200)
            .body("seats", hasSize(1))
            .body("seats[0].status", equalTo("AVAILABLE"));
    }

    @Test
    void doubleBook_sameSeat_returns409() {
        var venueId = createVenue();
        addSeats(venueId, List.of(21));
        var showId = scheduleShow(venueId);
        openShow(showId);
        var seatId = firstSeatId(showId);

        var request = Map.of(
            "showId", showId,
            "seatIds", List.of(seatId),
            "customerName", "Katherine Johnson",
            "customerEmail", "kj@example.com");

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/bookings")
        .then()
            .statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/bookings")
        .then()
            .statusCode(409)
            .body("title", equalTo("Seat unavailable"));
    }

    private UUID createVenue() {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Grand Hall", "address", "Main Street 1"))
        .when()
            .post("/venues")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getUUID("id");
    }

    private void addSeats(UUID venueId, List<Integer> seatNumbers) {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("rowLabel", "A", "seatNumbers", seatNumbers))
        .when()
            .post("/venues/{id}/seats", venueId)
        .then()
            .statusCode(200);
    }

    private UUID scheduleShow(UUID venueId) {
        var startsAt = LocalDateTime.now().plusDays(1).withNano(0);
        var endsAt = startsAt.plusHours(2);
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of(
                "venueId", venueId,
                "title", "Hamlet",
                "startsAt", startsAt.toString(),
                "endsAt", endsAt.toString(),
                "ticketPriceAmount", new BigDecimal("55.00"),
                "ticketPriceCurrency", "EUR"))
        .when()
            .post("/shows")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getUUID("id");
    }

    private void openShow(UUID showId) {
        given()
        .when()
            .post("/shows/{id}/open", showId)
        .then()
            .statusCode(200);
    }

    private UUID firstSeatId(UUID showId) {
        return given()
        .when()
            .get("/shows/{id}", showId)
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getUUID("seats[0].seatId");
    }
}
