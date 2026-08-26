package com.moviebooking.exception;

public class ShowtimeNotBookableException extends RuntimeException {
    public ShowtimeNotBookableException(String message) {
        super(message);
    }
}
