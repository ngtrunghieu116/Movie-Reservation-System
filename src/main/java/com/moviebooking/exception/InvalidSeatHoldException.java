package com.moviebooking.exception;

public class InvalidSeatHoldException extends RuntimeException {
    public InvalidSeatHoldException(String message) {
        super(message);
    }
}
