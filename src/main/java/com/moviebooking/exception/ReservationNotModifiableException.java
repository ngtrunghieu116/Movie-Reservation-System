package com.moviebooking.exception;

public class ReservationNotModifiableException extends RuntimeException {
    public ReservationNotModifiableException(String message) {
        super(message);
    }
}
