package dev.murilodcosta.url_shortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.URL;

@Schema(description = "Request payload for creating a shortened URL")
public record ShortenRequest(
    @Schema(description = "Original long URL to be shortened", example = "https://spring.io/projects/spring-boot")
    @NotBlank(message = "Original URL is required")
    @URL(message = "Original URL must be a valid URL")
    String url,

    @Schema(description = "Optional expiration duration in minutes. If omitted, the URL will not expire.", example = "60")
    @Positive(message = "Expiration time must be a positive value in minutes")
    Long expiresInMinutes
) {}
