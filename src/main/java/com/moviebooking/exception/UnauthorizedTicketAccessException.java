package com.moviebooking.exception;

public class UnauthorizedTicketAccessException extends RuntimeException {
    public UnauthorizedTicketAccessException(String message) {
        super(message);
    }
}
