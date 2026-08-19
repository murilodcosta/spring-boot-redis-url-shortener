package dev.murilodcosta.url_shortener.service;

import dev.murilodcosta.url_shortener.dto.ShortenRequest;
import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.exception.UrlExpiredException;
import dev.murilodcosta.url_shortener.exception.UrlNotFoundException;
import dev.murilodcosta.url_shortener.model.UrlMapping;
import dev.murilodcosta.url_shortener.repository.UrlMappingRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private UrlMappingRepository repository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SimpleMeterRegistry meterRegistry;
    private UrlShortenerService service;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:8080";
        meterRegistry = new SimpleMeterRegistry();
        service = new UrlShortenerService(repository, redisTemplate, meterRegistry);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should shorten URL successfully, generating Base62 shortCode and warming up Redis cache")
    void shouldShortenUrlSuccessfullyAndWarmUpCache() {
        ShortenRequest request = new ShortenRequest("https://google.com", 60L);

        UrlMapping initialEntity = UrlMapping.builder()
                .id(10L)
                .longUrl("https://google.com")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .build();

        when(repository.save(any(UrlMapping.class))).thenReturn(initialEntity);

        ShortenResponse response = service.shortenUrl(request, baseUrl);

        assertNotNull(response);
        assertEquals("a", response.shortCode());
        assertEquals("http://localhost:8080/a", response.shortUrl());
        assertEquals("https://google.com", response.longUrl());
        assertNotNull(response.expiresAt());

        // Verify repository saved twice (initial insert + shortCode update)
        verify(repository, times(2)).save(any(UrlMapping.class));

        // Verify Redis cache warm-up was called
        verify(valueOperations, times(1)).set(eq("url:a"), eq("https://google.com"), any(Duration.class));
    }

    @Test
    @DisplayName("Should resolve URL from Redis cache on Cache Hit without querying database and increment hit metric")
    void shouldResolveUrlFromCacheWhenCacheHit() {
        when(valueOperations.get("url:w7e")).thenReturn("https://cached-google.com");

        String resolvedUrl = service.resolveUrl("w7e");

        assertEquals("https://cached-google.com", resolvedUrl);

        // Verify PostgreSQL was NEVER queried (0 database hits!)
        verify(repository, never()).findByShortCode(anyString());

        // Verify Cache Hit metric was incremented
        assertEquals(1.0, meterRegistry.get("cache.access").tag("result", "hit").counter().count());
    }

    @Test
    @DisplayName("Should resolve URL from database and populate Redis cache on Cache Miss and increment miss metric")
    void shouldResolveUrlFromDatabaseAndPopulateCacheWhenCacheMiss() {
        when(valueOperations.get("url:1")).thenReturn(null); // Cache miss

        UrlMapping entity = UrlMapping.builder()
                .id(1L)
                .shortCode("1")
                .longUrl("https://spring.io")
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByShortCode("1")).thenReturn(Optional.of(entity));

        String resolvedUrl = service.resolveUrl("1");

        assertEquals("https://spring.io", resolvedUrl);

        // Verify database was queried
        verify(repository, times(1)).findByShortCode("1");

        // Verify Redis cache was populated with default TTL
        verify(valueOperations, times(1)).set(eq("url:1"), eq("https://spring.io"), eq(Duration.ofDays(7)));

        // Verify Cache Miss metric was incremented
        assertEquals(1.0, meterRegistry.get("cache.access").tag("result", "miss").counter().count());
    }

    @Test
    @DisplayName("Should throw UrlNotFoundException on Cache Miss when shortCode does not exist in database")
    void shouldThrowUrlNotFoundExceptionWhenCodeDoesNotExist() {
        when(valueOperations.get("url:invalid")).thenReturn(null);
        when(repository.findByShortCode("invalid")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> service.resolveUrl("invalid"));

        // Verify Redis was not populated
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should throw UrlExpiredException on Cache Miss when URL is expired")
    void shouldThrowUrlExpiredExceptionWhenUrlIsExpired() {
        when(valueOperations.get("url:2")).thenReturn(null);

        UrlMapping expiredEntity = UrlMapping.builder()
                .id(2L)
                .shortCode("2")
                .longUrl("https://expired.com")
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(repository.findByShortCode("2")).thenReturn(Optional.of(expiredEntity));

        assertThrows(UrlExpiredException.class, () -> service.resolveUrl("2"));

        // Verify Redis was not populated
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should gracefully fallback to PostgreSQL database when Redis throws exception")
    void shouldFallbackToDatabaseWhenRedisFails() {
        when(valueOperations.get("url:resilient")).thenThrow(new RuntimeException("Redis connection refused"));

        UrlMapping entity = UrlMapping.builder()
                .id(99L)
                .shortCode("resilient")
                .longUrl("https://resilient.io")
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByShortCode("resilient")).thenReturn(Optional.of(entity));

        String resolvedUrl = service.resolveUrl("resilient");

        assertEquals("https://resilient.io", resolvedUrl);
        verify(repository, times(1)).findByShortCode("resilient");
    }
}
