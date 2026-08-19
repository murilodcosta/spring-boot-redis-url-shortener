package dev.murilodcosta.url_shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickTrackingService {

    public static final String CLICK_PREFIX = "clicks:";

    private final StringRedisTemplate stringRedisTemplate;

    @Async("clickTrackingExecutor")
    public void registerClick(String shortCode) {
        try {
            stringRedisTemplate.opsForValue().increment(CLICK_PREFIX + shortCode);
        } catch (Exception ex) {
            // Best-effort: Click tracking failure must never impact the user experience
            log.warn("Failed to record click for shortCode {}. Error: {}", shortCode, ex.getMessage());
        }
    }
}
