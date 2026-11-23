package com.tenzin.TicketBook.dto;

import com.tenzin.TicketBook.entity.Show;

import java.time.LocalDateTime;

public record ShowResponse(Long id, String movieName, String venue, LocalDateTime startTime) {

    public static ShowResponse from(Show show) {
        return new ShowResponse(show.getId(), show.getMovieName(), show.getVenue(), show.getStartTime());
    }
}
