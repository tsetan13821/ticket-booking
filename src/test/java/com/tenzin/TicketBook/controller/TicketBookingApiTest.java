package com.tenzin.TicketBook.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests of all six endpoints and the 400 / 404 / 409 error responses. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketBookingApiTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------- shows ----------

    @Test
    void createShow_generatesSeatGrid() throws Exception {
        long showId = createShow(3, 4);

        mockMvc.perform(get("/shows/{id}/seats", showId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$[11].seatNumber").value("C4"))
                .andExpect(jsonPath("$[*].status", everyItem(is("AVAILABLE"))))
                .andExpect(jsonPath("$[0].price").value(250.0));
    }

    @Test
    void createShow_defaultsToFiveRowsOfTen() throws Exception {
        long showId = createShow(null, null);

        mockMvc.perform(get("/shows/{id}/seats", showId))
                .andExpect(jsonPath("$.length()").value(50))
                .andExpect(jsonPath("$[49].seatNumber").value("E10"));
    }

    @Test
    void createShow_returnsLocationAndAppearsInList() throws Exception {
        String body = showJson("List Test", "Hall 9", future(), "199.50", null, null);

        String response = mockMvc.perform(post("/shows").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/shows/")))
                .andExpect(jsonPath("$.movieName").value("List Test"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));
    }

    @Test
    void createShow_invalidBody_returns400WithFieldErrors() throws Exception {
        String body = showJson("", "Hall", "2020-01-01T10:00:00", "-5", 30, null);

        mockMvc.perform(post("/shows").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.movieName").exists())
                .andExpect(jsonPath("$.errors.startTime").exists())
                .andExpect(jsonPath("$.errors.seatPrice").exists())
                .andExpect(jsonPath("$.errors.rows").exists());
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/shows").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void seatMap_unknownShow_returns404() throws Exception {
        mockMvc.perform(get("/shows/{id}/seats", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Show 999999 not found"));
    }

    @Test
    void seatMap_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/shows/abc/seats"))
                .andExpect(status().isBadRequest());
    }

    // ---------- bookings ----------

    @Test
    void fullBookingLifecycle() throws Exception {
        long showId = createShow(1, 5);
        List<Integer> seatIds = seatIds(showId);
        Integer a1 = seatIds.get(0);
        Integer a2 = seatIds.get(1);

        // Book A1 + A2.
        String response = book("Alice", showId, a1, a2)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/bookings/")))
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.showId").value(showId))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andExpect(jsonPath("$.seats[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$.totalAmount").value(500.0))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        Integer bookingId = JsonPath.read(response, "$.id");

        // Seats now show as BOOKED.
        mockMvc.perform(get("/shows/{id}/seats", showId))
                .andExpect(jsonPath("$[0].status").value("BOOKED"))
                .andExpect(jsonPath("$[1].status").value("BOOKED"))
                .andExpect(jsonPath("$[2].status").value("AVAILABLE"));

        // Anyone else trying A2 gets 409.
        book("Bob", showId, a2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Seats [A2] are already booked"));

        // View the booking.
        mockMvc.perform(get("/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Cancel frees the seats but the booking keeps its seat list.
        mockMvc.perform(delete("/bookings/{id}", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.seats.length()").value(2));

        mockMvc.perform(get("/shows/{id}/seats", showId))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));

        // Cancelling twice is a conflict.
        mockMvc.perform(delete("/bookings/{id}", bookingId))
                .andExpect(status().isConflict());

        // The freed seat can be booked again.
        book("Bob", showId, a2).andExpect(status().isCreated());
    }

    @Test
    void booking_isAllOrNothing() throws Exception {
        long showId = createShow(1, 3);
        List<Integer> seatIds = seatIds(showId);

        book("Alice", showId, seatIds.get(1)).andExpect(status().isCreated());

        // A1 is free but A2 is taken: the whole request fails and A1 stays free.
        book("Bob", showId, seatIds.get(0), seatIds.get(1)).andExpect(status().isConflict());

        mockMvc.perform(get("/shows/{id}/seats", showId))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void booking_duplicateSeatIdsAreBookedOnce() throws Exception {
        long showId = createShow(1, 2);
        Integer a1 = seatIds(showId).getFirst();

        book("Alice", showId, a1, a1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seats.length()").value(1))
                .andExpect(jsonPath("$.totalAmount").value(250.0));
    }

    @Test
    void booking_seatFromAnotherShow_returns404() throws Exception {
        long showA = createShow(1, 2);
        long showB = createShow(1, 2);
        Integer seatOfB = seatIds(showB).getFirst();

        book("Alice", showA, seatOfB)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("not found for show " + showA)));
    }

    @Test
    void booking_unknownShow_returns404() throws Exception {
        book("Alice", 999_999L, 1).andExpect(status().isNotFound());
    }

    @Test
    void booking_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": " ", "showId": null, "seatIds": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.customerName").exists())
                .andExpect(jsonPath("$.errors.showId").exists())
                .andExpect(jsonPath("$.errors.seatIds").exists());
    }

    @Test
    void getAndCancel_unknownBooking_return404() throws Exception {
        mockMvc.perform(get("/bookings/{id}", 999_999)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/bookings/{id}", 999_999)).andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private long createShow(Integer rows, Integer seatsPerRow) throws Exception {
        String body = showJson("Test Movie", "Test Hall", future(), "250.00", rows, seatsPerRow);
        String response = mockMvc.perform(post("/shows").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private List<Integer> seatIds(long showId) throws Exception {
        String response = mockMvc.perform(get("/shows/{id}/seats", showId))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$[*].id");
    }

    private ResultActions book(String customer, long showId, Integer... seatIds) throws Exception {
        String ids = String.join(", ", java.util.Arrays.stream(seatIds).map(String::valueOf).toList());
        return mockMvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerName": "%s", "showId": %d, "seatIds": [%s]}
                        """.formatted(customer, showId, ids)));
    }

    private static String showJson(String movie, String venue, String startTime, String price,
                                   Integer rows, Integer seatsPerRow) {
        return """
                {"movieName": "%s", "venue": "%s", "startTime": "%s", "seatPrice": %s, "rows": %s, "seatsPerRow": %s}
                """.formatted(movie, venue, startTime, price, rows, seatsPerRow);
    }

    private static String future() {
        return LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
