package dev.murilodcosta.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.URL;

public record ShortenRequest(
    @NotBlank(message = "Original URL is required")
    @URL(message = "Original URL must be a valid URL")
    String url,

    @Positive(message = "Expiration time must be a positive value in minutes")
    Long expiresInMinutes
) {}
