package dev.murilodcosta.url_shortener.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UrlMappingTest {

    @Test
    @DisplayName("Should not be expired when expiresAt is null")
    void shouldNotBeExpiredWhenExpiresAtIsNull() {
        UrlMapping mapping = UrlMapping.builder()
                .longUrl("https://google.com")
                .expiresAt(null)
                .build();

        assertFalse(mapping.isExpired());
    }

    @Test
    @DisplayName("Should not be expired when expiresAt is in the future")
    void shouldNotBeExpiredWhenExpiresAtIsInTheFuture() {
        UrlMapping mapping = UrlMapping.builder()
                .longUrl("https://google.com")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        assertFalse(mapping.isExpired());
    }

    @Test
    @DisplayName("Should be expired when expiresAt is in the past")
    void shouldBeExpiredWhenExpiresAtIsInThePast() {
        UrlMapping mapping = UrlMapping.builder()
                .longUrl("https://google.com")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        assertTrue(mapping.isExpired());
    }
}