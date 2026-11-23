package com.tenzin.TicketBook.service;

import com.tenzin.TicketBook.dto.BookingRequest;
import com.tenzin.TicketBook.dto.BookingResponse;
import com.tenzin.TicketBook.entity.Booking;
import com.tenzin.TicketBook.entity.BookingStatus;
import com.tenzin.TicketBook.entity.Seat;
import com.tenzin.TicketBook.entity.SeatStatus;
import com.tenzin.TicketBook.entity.Show;
import com.tenzin.TicketBook.exception.ConflictException;
import com.tenzin.TicketBook.exception.ResourceNotFoundException;
import com.tenzin.TicketBook.repository.BookingRepository;
import com.tenzin.TicketBook.repository.SeatRepository;
import com.tenzin.TicketBook.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;

    /**
     * Books all requested seats or none of them.
     * <ol>
     *   <li>Lock the requested seats with SELECT ... FOR UPDATE, ordered by id.</li>
     *   <li>If any seat is missing or already BOOKED, throw: the whole transaction rolls back.</li>
     *   <li>Create the booking, mark the seats BOOKED and compute the total.</li>
     * </ol>
     * A second request for the same seat blocks on the row lock until the first commits,
     * then sees the seat as BOOKED and fails with 409.
     */
    @Transactional
    public BookingResponse book(BookingRequest request) {
        Show show = showRepository.findById(request.showId())
                .orElseThrow(() -> ResourceNotFoundException.show(request.showId()));

        List<Long> seatIds = request.seatIds().stream().distinct().sorted().toList();
        List<Seat> seats = seatRepository.findAndLockByShowIdAndIds(show.getId(), seatIds);

        if (seats.size() != seatIds.size()) {
            Set<Long> found = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            List<Long> missing = seatIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("Seats " + missing + " not found for show " + show.getId());
        }

        List<String> alreadyBooked = seats.stream()
                .filter(Seat::isBooked)
                .map(Seat::getSeatNumber)
                .toList();
        if (!alreadyBooked.isEmpty()) {
            throw new ConflictException("Seats " + alreadyBooked + " are already booked");
        }

        BigDecimal total = seats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        seats.forEach(seat -> seat.setStatus(SeatStatus.BOOKED));

        Booking booking = bookingRepository.save(
                new Booking(request.customerName().trim(), show, seats, total));
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .map(BookingResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.booking(bookingId));
    }

    /**
     * The reverse of {@link #book}: lock the booking, lock its seats in id order,
     * set them back to AVAILABLE and mark the booking CANCELLED.
     */
    @Transactional
    public BookingResponse cancel(Long bookingId) {
        Booking booking = bookingRepository.findAndLockById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.booking(bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException("Booking " + bookingId + " is already cancelled");
        }

        List<Long> seatIds = bookingRepository.findSeatIdsByBookingId(bookingId);
        seatRepository.findAndLockByIds(seatIds)
                .forEach(seat -> seat.setStatus(SeatStatus.AVAILABLE));
        booking.setStatus(BookingStatus.CANCELLED);

        return BookingResponse.from(booking);
    }
}
