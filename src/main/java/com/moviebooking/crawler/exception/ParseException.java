package com.moviebooking.crawler.exception;

public class ParseException extends CrawlerException {
    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
