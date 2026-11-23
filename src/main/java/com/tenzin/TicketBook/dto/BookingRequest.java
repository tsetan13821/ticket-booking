package com.tenzin.TicketBook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotNull @Positive Long showId,
        @NotEmpty @Size(max = 10) List<@NotNull @Positive Long> seatIds
) {
}
