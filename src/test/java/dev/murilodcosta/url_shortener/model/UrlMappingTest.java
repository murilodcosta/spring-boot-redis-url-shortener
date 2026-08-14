package dev.murilodcosta.url_shortener.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class UrlMappingTest {

    @Test
    @DisplayName("Test isExpired method when expiresAt is null")
    void testIsExpiredWhenExpiresAtIsNull() {
        UrlMapping urlMapping = UrlMapping.builder()
                .longUrl("https://example.com")
                .expiresAt(null)
                .build();
        assertFalse(urlMapping.isExpired(), "Expected isExpired to return false when expiresAt is null");
    }

    @Test
    @DisplayName("Test isExpired method when expiresAt is in the future")
    void testIsExpiredWhenExpiresAtIsInTheFuture() {
        UrlMapping urlMapping = UrlMapping.builder()
                .longUrl("https://example.com")
                .expiresAt(java.time.LocalDateTime.now().plusDays(1))
                .build();
        assertFalse(urlMapping.isExpired(), "Expected isExpired to return false when expiresAt is in the future");
    }

    @Test
    @DisplayName("Test isExpired method when expiresAt is in the past")
    void testIsExpiredWhenExpiresAtIsInThePast() {
        UrlMapping urlMapping = UrlMapping.builder()
                .longUrl("https://example.com")
                .expiresAt(java.time.LocalDateTime.now().minusDays(1))
                .build();
        assertTrue(urlMapping.isExpired(), "Expected isExpired to return true when expiresAt is in the past");
    }
}