package com.tenzin.TicketBook.repository;

import com.tenzin.TicketBook.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findAllByOrderByStartTimeAsc();
}
