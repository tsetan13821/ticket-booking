package com.tenzin.TicketBook.repository;

import com.tenzin.TicketBook.entity.Booking;
import com.tenzin.TicketBook.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Locks the booking row so two concurrent cancels cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findAndLockById(@Param("id") Long id);

    @Query("select s.id from Booking b join b.seats s where b.id = :bookingId order by s.id")
    List<Long> findSeatIdsByBookingId(@Param("bookingId") Long bookingId);

    long countByShowIdAndStatus(Long showId, BookingStatus status);
}
