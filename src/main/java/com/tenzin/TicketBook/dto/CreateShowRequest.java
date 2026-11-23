package com.tenzin.TicketBook.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Creates a show and generates a rows x seatsPerRow seat grid (A1..A10, B1..B10, ...).
 * rows and seatsPerRow are optional and default to 5 x 10.
 */
public record CreateShowRequest(
        @NotBlank @Size(max = 200) String movieName,
        @NotBlank @Size(max = 200) String venue,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal seatPrice,
        @Min(1) @Max(26) Integer rows,
        @Min(1) @Max(50) Integer seatsPerRow
) {
    public static final int DEFAULT_ROWS = 5;
    public static final int DEFAULT_SEATS_PER_ROW = 10;

    public int rowsOrDefault() {
        return rows != null ? rows : DEFAULT_ROWS;
    }

    public int seatsPerRowOrDefault() {
        return seatsPerRow != null ? seatsPerRow : DEFAULT_SEATS_PER_ROW;
    }
}
