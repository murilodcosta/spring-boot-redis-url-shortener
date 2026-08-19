package dev.murilodcosta.url_shortener.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("Should allow request when token is available in the bucket")
    void shouldAllowRequestWhenTokenIsAvailable() {
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("ratelimit:shorten:127.0.0.1")),
                eq("10"),
                eq("0.16666666666666666"),
                any(String.class)
        )).thenReturn(1L);

        boolean allowed = rateLimiterService.tryConsume("127.0.0.1", "shorten", 10, 10.0 / 60.0);

        assertTrue(allowed);
    }

    @Test
    @DisplayName("Should reject request when bucket has no tokens left")
    void shouldRejectRequestWhenBucketIsEmpty() {
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("ratelimit:shorten:127.0.0.1")),
                eq("10"),
                eq("0.16666666666666666"),
                any(String.class)
        )).thenReturn(0L);

        boolean allowed = rateLimiterService.tryConsume("127.0.0.1", "shorten", 10, 10.0 / 60.0);

        assertFalse(allowed);
    }

    @Test
    @DisplayName("Should fail open and allow request when Redis throws an exception")
    void shouldFailOpenWhenRedisThrowsException() {
        when(stringRedisTemplate.execute(
                any(RedisScript.class),
                eq(List.of("ratelimit:shorten:127.0.0.1")),
                eq("10"),
                eq("0.16666666666666666"),
                any(String.class)
        )).thenThrow(new RuntimeException("Redis connection timeout"));

        boolean allowed = rateLimiterService.tryConsume("127.0.0.1", "shorten", 10, 10.0 / 60.0);

        assertTrue(allowed, "Rate limiter should fail-open when Redis is unreachable");
    }
}
