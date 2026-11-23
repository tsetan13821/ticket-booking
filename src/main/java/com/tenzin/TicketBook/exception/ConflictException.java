package com.tenzin.TicketBook.exception;

/** Mapped to 409 Conflict: the request is valid but clashes with the current state (e.g. seat already booked). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
