package dev.murilodcosta.url_shortener.exception;

public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String shortCode) {
        super("A URL encurtada com o código '" + shortCode + "' expirou");
    }
}
