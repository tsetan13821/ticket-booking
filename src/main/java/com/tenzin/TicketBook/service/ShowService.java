package com.tenzin.TicketBook.service;

import com.tenzin.TicketBook.dto.CreateShowRequest;
import com.tenzin.TicketBook.dto.SeatResponse;
import com.tenzin.TicketBook.dto.ShowResponse;
import com.tenzin.TicketBook.entity.Seat;
import com.tenzin.TicketBook.entity.Show;
import com.tenzin.TicketBook.exception.ResourceNotFoundException;
import com.tenzin.TicketBook.repository.SeatRepository;
import com.tenzin.TicketBook.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    /** Creates the show and its full seat grid in one transaction. */
    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {
        Show show = showRepository.save(
                new Show(request.movieName().trim(), request.venue().trim(), request.startTime()));

        int rows = request.rowsOrDefault();
        int seatsPerRow = request.seatsPerRowOrDefault();
        List<Seat> seats = new ArrayList<>(rows * seatsPerRow);
        for (int row = 0; row < rows; row++) {
            char rowLetter = (char) ('A' + row);
            for (int number = 1; number <= seatsPerRow; number++) {
                seats.add(new Seat(show, rowLetter + String.valueOf(number), request.seatPrice()));
            }
        }
        seatRepository.saveAll(seats);

        return ShowResponse.from(show);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> listShows() {
        return showRepository.findAllByOrderByStartTimeAsc().stream()
                .map(ShowResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatMap(Long showId) {
        if (!showRepository.existsById(showId)) {
            throw ResourceNotFoundException.show(showId);
        }
        return seatRepository.findByShowIdOrderByIdAsc(showId).stream()
                .map(SeatResponse::from)
                .toList();
    }
}
