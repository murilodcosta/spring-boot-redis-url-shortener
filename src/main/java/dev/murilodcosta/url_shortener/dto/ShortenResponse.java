package dev.murilodcosta.url_shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response payload containing shortened URL details")
public record ShortenResponse(
    @Schema(description = "Base62 encoded short code", example = "1")
    String shortCode,

    @Schema(description = "Full shortened URL link", example = "http://localhost:8080/1")
    String shortUrl,

    @Schema(description = "Original destination URL", example = "https://spring.io/projects/spring-boot")
    String longUrl,

    @Schema(description = "Creation timestamp")
    LocalDateTime createdAt,

    @Schema(description = "Expiration timestamp (null if URL never expires)")
    LocalDateTime expiresAt
) {}
