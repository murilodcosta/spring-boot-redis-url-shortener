package dev.murilodcosta.url_shortener.job;

import dev.murilodcosta.url_shortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickCountSyncJobTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @InjectMocks
    private ClickCountSyncJob clickCountSyncJob;

    @Test
    @DisplayName("Should sync click counts from Redis to PostgreSQL using getAndDelete")
    void shouldSyncClickCountsSuccessfully() {
        Set<String> keys = Set.of("clicks:w7e", "clicks:abc");
        when(stringRedisTemplate.keys("clicks:*")).thenReturn(keys);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        when(valueOperations.getAndDelete("clicks:w7e")).thenReturn("42");
        when(valueOperations.getAndDelete("clicks:abc")).thenReturn("15");

        clickCountSyncJob.syncClickCounts();

        verify(urlMappingRepository, times(1)).incrementClickCount("w7e", 42L);
        verify(urlMappingRepository, times(1)).incrementClickCount("abc", 15L);
    }

    @Test
    @DisplayName("Should do nothing when no click keys exist in Redis")
    void shouldDoNothingWhenNoKeysExist() {
        when(stringRedisTemplate.keys("clicks:*")).thenReturn(Collections.emptySet());

        clickCountSyncJob.syncClickCounts();

        verifyNoInteractions(urlMappingRepository);
    }
}
