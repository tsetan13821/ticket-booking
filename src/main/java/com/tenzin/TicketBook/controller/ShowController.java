package com.tenzin.TicketBook.controller;

import com.tenzin.TicketBook.dto.CreateShowRequest;
import com.tenzin.TicketBook.dto.SeatResponse;
import com.tenzin.TicketBook.dto.ShowResponse;
import com.tenzin.TicketBook.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    public ResponseEntity<ShowResponse> createShow(@Valid @RequestBody CreateShowRequest request) {
        ShowResponse show = showService.createShow(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}/seats")
                .buildAndExpand(show.id())
                .toUri();
        return ResponseEntity.created(location).body(show);
    }

    @GetMapping
    public List<ShowResponse> listShows() {
        return showService.listShows();
    }

    @GetMapping("/{id}/seats")
    public List<SeatResponse> getSeatMap(@PathVariable Long id) {
        return showService.getSeatMap(id);
    }
}
