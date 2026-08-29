package com.moviebooking.exception;

public class ProductInactiveException extends RuntimeException {
    public ProductInactiveException(String message) {
        super(message);
    }
}
