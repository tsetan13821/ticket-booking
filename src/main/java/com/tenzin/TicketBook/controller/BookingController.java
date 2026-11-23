package com.tenzin.TicketBook.controller;

import com.tenzin.TicketBook.dto.BookingRequest;
import com.tenzin.TicketBook.dto.BookingResponse;
import com.tenzin.TicketBook.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> book(@Valid @RequestBody BookingRequest request) {
        BookingResponse booking = bookingService.book(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(booking.id())
                .toUri();
        return ResponseEntity.created(location).body(booking);
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    /** Cancels the booking and frees its seats. Returns the cancelled booking. */
    @DeleteMapping("/{id}")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }
}
