package dev.murilodcosta.url_shortener.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickTrackingServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ClickTrackingService clickTrackingService;

    @Test
    @DisplayName("Should increment click counter in Redis for given shortCode")
    void shouldIncrementClickCounterInRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        clickTrackingService.registerClick("w7e");

        verify(valueOperations, times(1)).increment("clicks:w7e");
    }

    @Test
    @DisplayName("Should gracefully handle Redis failure without throwing exception")
    void shouldHandleRedisFailureGracefully() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("clicks:w7e")).thenThrow(new RuntimeException("Redis unavailable"));

        // Should not throw any exception
        clickTrackingService.registerClick("w7e");

        verify(valueOperations, times(1)).increment("clicks:w7e");
    }
}
