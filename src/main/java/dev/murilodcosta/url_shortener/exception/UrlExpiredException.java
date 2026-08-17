package dev.murilodcosta.url_shortener.exception;

public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String shortCode) {
        super("The shortened URL with code '" + shortCode + "' has expired");
    }
}
