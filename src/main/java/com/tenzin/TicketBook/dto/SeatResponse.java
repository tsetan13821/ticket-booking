package com.tenzin.TicketBook.dto;

import com.tenzin.TicketBook.entity.Seat;
import com.tenzin.TicketBook.entity.SeatStatus;

import java.math.BigDecimal;

public record SeatResponse(Long id, String seatNumber, BigDecimal price, SeatStatus status) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getPrice(), seat.getStatus());
    }
}
