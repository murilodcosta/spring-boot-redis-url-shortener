package dev.murilodcosta.url_shortener.job;

import dev.murilodcosta.url_shortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static dev.murilodcosta.url_shortener.service.ClickTrackingService.CLICK_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClickCountSyncJob {

    private final StringRedisTemplate stringRedisTemplate;
    private final UrlMappingRepository urlMappingRepository;

    @Scheduled(fixedRate = 60_000) // Runs every 1 minute
    @Transactional
    public void syncClickCounts() {
        try {
            Set<String> keys = stringRedisTemplate.keys(CLICK_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return;
            }

            int syncedCount = 0;
            for (String key : keys) {
                String shortCode = key.substring(CLICK_PREFIX.length());
                // Atomic get and delete ensures no clicks are lost during batch synchronization
                String value = stringRedisTemplate.opsForValue().getAndDelete(key);
                if (value != null) {
                    try {
                        long increment = Long.parseLong(value);
                        if (increment > 0) {
                            urlMappingRepository.incrementClickCount(shortCode, increment);
                            syncedCount++;
                        }
                    } catch (NumberFormatException nfe) {
                        log.warn("Invalid click count format for key {}: {}", key, value);
                    }
                }
            }

            if (syncedCount > 0) {
                log.info("Successfully synchronized click counts for {} URLs into PostgreSQL", syncedCount);
            }
        } catch (Exception ex) {
            log.error("Error during scheduled click counts sync: {}", ex.getMessage(), ex);
        }
    }
}
