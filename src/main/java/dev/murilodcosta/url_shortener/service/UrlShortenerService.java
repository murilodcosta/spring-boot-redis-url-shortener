package dev.murilodcosta.url_shortener.service;

import dev.murilodcosta.url_shortener.dto.ShortenRequest;
import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.exception.UrlExpiredException;
import dev.murilodcosta.url_shortener.exception.UrlNotFoundException;
import dev.murilodcosta.url_shortener.model.UrlMapping;
import dev.murilodcosta.url_shortener.repository.UrlMappingRepository;
import dev.murilodcosta.url_shortener.util.Base62Encoder;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final String CACHE_KEY_PREFIX = "url:";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofDays(7);

    private final UrlMappingRepository urlMappingRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, String baseUrl) {
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = request.expiresInMinutes() != null
                ? createdAt.plusMinutes(request.expiresInMinutes())
                : null;

        UrlMapping urlMapping = UrlMapping.builder()
                .longUrl(request.url())
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();

        // 1. Save the initial entity to obtain the PostgreSQL sequence generated ID
        UrlMapping saved = urlMappingRepository.save(urlMapping);

        // 2. Convert generated ID into Base62 short code
        String shortCode = Base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);

        // 3. Update the entity with the generated short code
        urlMappingRepository.save(saved);

        // 4. Cache warm-up: populate Redis immediately upon creation
        populateCache(shortCode, saved.getLongUrl(), expiresAt);

        String shortUrl = baseUrl + "/" + shortCode;

        return new ShortenResponse(
                shortCode,
                shortUrl,
                saved.getLongUrl(),
                saved.getCreatedAt(),
                saved.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public String resolveUrl(String shortCode) {
        String cacheKey = CACHE_KEY_PREFIX + shortCode;

        // 1. Cache-Aside: Check Redis cache first
        try {
            String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUrl != null) {
                meterRegistry.counter("cache.access", "result", "hit").increment();
                log.debug("Cache hit for shortCode: {}", shortCode);
                return cachedUrl;
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable during resolveUrl for key {}. Falling back to PostgreSQL. Error: {}", cacheKey, ex.getMessage());
        }

        meterRegistry.counter("cache.access", "result", "miss").increment();
        log.debug("Cache miss for shortCode: {}. Querying PostgreSQL.", shortCode);

        // 2. Cache Miss: Query PostgreSQL database
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (urlMapping.isExpired()) {
            throw new UrlExpiredException(shortCode);
        }

        // 3. Populate Redis cache with proper TTL for future read requests
        populateCache(shortCode, urlMapping.getLongUrl(), urlMapping.getExpiresAt());

        return urlMapping.getLongUrl();
    }

    private void populateCache(String shortCode, String longUrl, LocalDateTime expiresAt) {
        try {
            String cacheKey = CACHE_KEY_PREFIX + shortCode;
            if (expiresAt != null) {
                Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(cacheKey, longUrl, ttl);
                }
            } else {
                redisTemplate.opsForValue().set(cacheKey, longUrl, DEFAULT_CACHE_TTL);
            }
        } catch (Exception ex) {
            log.warn("Failed to populate Redis cache for shortCode: {}. Error: {}", shortCode, ex.getMessage());
        }
    }
}
