package dev.murilodcosta.url_shortener.service;

import dev.murilodcosta.url_shortener.dto.ShortenRequest;
import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.exception.UrlExpiredException;
import dev.murilodcosta.url_shortener.exception.UrlNotFoundException;
import dev.murilodcosta.url_shortener.model.UrlMapping;
import dev.murilodcosta.url_shortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @InjectMocks
    private UrlShortenerService service;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:8080";
    }

    @Test
    @DisplayName("Should shorten URL successfully when valid request is provided")
    void shouldShortenUrlSuccessfully() {
        ShortenRequest request = new ShortenRequest("https://google.com", null);

        UrlMapping initialEntity = UrlMapping.builder()
                .id(10L)
                .longUrl("https://google.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.save(any(UrlMapping.class))).thenReturn(initialEntity);

        ShortenResponse response = service.shortenUrl(request, baseUrl);

        assertNotNull(response);
        assertEquals("a", response.shortCode());
        assertEquals("http://localhost:8080/a", response.shortUrl());
        assertEquals("https://google.com", response.longUrl());
        assertNull(response.expiresAt());

        verify(repository, times(2)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("Should resolve short code successfully when it exists and is not expired")
    void shouldResolveShortCodeSuccessfully() {
        UrlMapping entity = UrlMapping.builder()
                .id(1L)
                .shortCode("1")
                .longUrl("https://spring.io")
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByShortCode("1")).thenReturn(Optional.of(entity));

        String resolvedUrl = service.resolveUrl("1");

        assertEquals("https://spring.io", resolvedUrl);
    }

    @Test
    @DisplayName("Should throw UrlNotFoundException when short code does not exist")
    void shouldThrowUrlNotFoundExceptionWhenCodeDoesNotExist() {
        when(repository.findByShortCode("invalid")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.resolveUrl("invalid"));
    }

    @Test
    @DisplayName("Should throw UrlExpiredException when short code is expired")
    void shouldThrowUrlExpiredExceptionWhenUrlIsExpired() {
        UrlMapping expiredEntity = UrlMapping.builder()
                .id(2L)
                .shortCode("2")
                .longUrl("https://expired.com")
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(repository.findByShortCode("2")).thenReturn(Optional.of(expiredEntity));

        assertThrows(UrlExpiredException.class, () -> service.resolveUrl("2"));
    }
}
