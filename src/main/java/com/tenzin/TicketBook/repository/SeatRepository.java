package com.tenzin.TicketBook.repository;

import com.tenzin.TicketBook.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowIdOrderByIdAsc(Long showId);

    /**
     * Locks the requested seats of a show with SELECT ... FOR UPDATE.
     * Rows are always locked in id order, so two overlapping requests
     * cannot each grab half of the seats and deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.show.id = :showId and s.id in :seatIds order by s.id")
    List<Seat> findAndLockByShowIdAndIds(@Param("showId") Long showId,
                                         @Param("seatIds") Collection<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id in :seatIds order by s.id")
    List<Seat> findAndLockByIds(@Param("seatIds") Collection<Long> seatIds);
}
