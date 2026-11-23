package com.tenzin.TicketBook.exception;

/** Mapped to 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException show(Long id) {
        return new ResourceNotFoundException("Show " + id + " not found");
    }

    public static ResourceNotFoundException booking(Long id) {
        return new ResourceNotFoundException("Booking " + id + " not found");
    }
}
