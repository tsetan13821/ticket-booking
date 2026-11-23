package com.tenzin.TicketBook.dto;

import com.tenzin.TicketBook.entity.Booking;
import com.tenzin.TicketBook.entity.BookingStatus;
import com.tenzin.TicketBook.entity.Seat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id,
        String customerName,
        Long showId,
        String movieName,
        List<BookedSeat> seats,
        BigDecimal totalAmount,
        BookingStatus status,
        Instant createdAt
) {

    public record BookedSeat(Long id, String seatNumber, BigDecimal price) {
        static BookedSeat from(Seat seat) {
            return new BookedSeat(seat.getId(), seat.getSeatNumber(), seat.getPrice());
        }
    }

    /** Must be called inside a transaction: show and seats are lazy. */
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCustomerName(),
                booking.getShow().getId(),
                booking.getShow().getMovieName(),
                booking.getSeats().stream().map(BookedSeat::from).toList(),
                booking.getTotalAmount(),
                booking.getStatus(),
                booking.getCreatedAt());
    }
}
