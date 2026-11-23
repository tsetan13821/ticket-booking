package com.tenzin.TicketBook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String movieName;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime startTime;

    public Show(String movieName, String venue, LocalDateTime startTime) {
        this.movieName = movieName;
        this.venue = venue;
        this.startTime = startTime;
    }
}
