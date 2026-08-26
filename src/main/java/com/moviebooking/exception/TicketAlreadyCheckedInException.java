package com.moviebooking.exception;

public class TicketAlreadyCheckedInException extends RuntimeException {
    public TicketAlreadyCheckedInException(String message) {
        super(message);
    }
}
