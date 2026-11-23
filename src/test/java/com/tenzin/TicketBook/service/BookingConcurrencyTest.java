package com.tenzin.TicketBook.service;

import com.tenzin.TicketBook.dto.BookingRequest;
import com.tenzin.TicketBook.dto.CreateShowRequest;
import com.tenzin.TicketBook.dto.SeatResponse;
import com.tenzin.TicketBook.entity.BookingStatus;
import com.tenzin.TicketBook.entity.SeatStatus;
import com.tenzin.TicketBook.exception.ConflictException;
import com.tenzin.TicketBook.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the pessimistic locking works: many threads race for the same seats
 * and exactly one booking wins. Not @Transactional on purpose, so every
 * booking runs in its own real transaction, just like concurrent HTTP requests.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {

    private static final int THREADS = 10;

    @Autowired
    private ShowService showService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void tenThreadsBookingTheSameSeat_exactlyOneSucceeds() throws Exception {
        Long showId = createShow();
        Long seatId = seatIds(showId).getFirst();

        RaceResult result = race(i -> new BookingRequest("customer-" + i, showId, List.of(seatId)));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.conflicts()).isEqualTo(THREADS - 1);
        assertThat(result.unexpected()).isEmpty();
        assertThat(seatStatus(showId, seatId)).isEqualTo(SeatStatus.BOOKED);
        assertThat(bookingRepository.countByShowIdAndStatus(showId, BookingStatus.CONFIRMED)).isEqualTo(1);
    }

    /**
     * Half the threads ask for [A1, A2], the other half for [A2, A1].
     * Because seats are always locked in id order, this cannot deadlock,
     * and the all-or-nothing transaction means exactly one request books both seats.
     */
    @Test
    void overlappingMultiSeatRequests_noDeadlockAndExactlyOneSucceeds() throws Exception {
        Long showId = createShow();
        List<Long> seatIds = seatIds(showId);
        Long first = seatIds.get(0);
        Long second = seatIds.get(1);

        RaceResult result = race(i -> new BookingRequest("customer-" + i, showId,
                i % 2 == 0 ? List.of(first, second) : List.of(second, first)));

        assertThat(result.successes()).isEqualTo(1);
        assertThat(result.conflicts()).isEqualTo(THREADS - 1);
        assertThat(result.unexpected()).isEmpty();
        assertThat(seatStatus(showId, first)).isEqualTo(SeatStatus.BOOKED);
        assertThat(seatStatus(showId, second)).isEqualTo(SeatStatus.BOOKED);
        assertThat(bookingRepository.countByShowIdAndStatus(showId, BookingStatus.CONFIRMED)).isEqualTo(1);
    }

    private RaceResult race(IntFunction<BookingRequest> requestForThread) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                BookingRequest request = requestForThread.apply(i);
                Callable<Boolean> task = () -> {
                    ready.countDown();
                    start.await();
                    try {
                        bookingService.book(request);
                        return true;
                    } catch (ConflictException expected) {
                        return false;
                    }
                };
                futures.add(pool.submit(task));
            }

            // Release every thread at the same moment to maximise contention.
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successes = 0;
            int conflicts = 0;
            List<Throwable> unexpected = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                try {
                    if (future.get(30, TimeUnit.SECONDS)) {
                        successes++;
                    } else {
                        conflicts++;
                    }
                } catch (java.util.concurrent.ExecutionException e) {
                    unexpected.add(e.getCause());
                }
            }
            return new RaceResult(successes, conflicts, unexpected);
        } finally {
            pool.shutdownNow();
        }
    }

    private Long createShow() {
        return showService.createShow(new CreateShowRequest(
                "Concurrency Test", "Test Hall",
                LocalDateTime.now().plusDays(1), new BigDecimal("200.00"), 1, 5)).id();
    }

    private List<Long> seatIds(Long showId) {
        return showService.getSeatMap(showId).stream().map(SeatResponse::id).toList();
    }

    private SeatStatus seatStatus(Long showId, Long seatId) {
        return showService.getSeatMap(showId).stream()
                .filter(seat -> seat.id().equals(seatId))
                .findFirst()
                .orElseThrow()
                .status();
    }

    private record RaceResult(int successes, int conflicts, List<Throwable> unexpected) {
    }
}
