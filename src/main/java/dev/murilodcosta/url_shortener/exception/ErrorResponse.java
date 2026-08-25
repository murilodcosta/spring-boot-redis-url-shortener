package dev.murilodcosta.url_shortener.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Standard RFC 7807 compatible error response")
public record ErrorResponse(
    @Schema(description = "HTTP status code", example = "400")
    int status,

    @Schema(description = "HTTP error title", example = "Bad Request")
    String error,

    @Schema(description = "Detailed error message", example = "Validation error in the provided fields")
    String message,

    @Schema(description = "Timestamp when the error occurred")
    LocalDateTime timestamp,

    @Schema(description = "Field-level validation error map (if applicable)")
    Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now(), null);
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> validationErrors) {
        this(status, error, message, LocalDateTime.now(), validationErrors);
    }
}
