package dev.murilodcosta.url_shortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<Long> tokenBucketScript = loadTokenBucketScript();

    private static RedisScript<Long> loadTokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        script.setResultType(Long.class);
        return script;
    }

    public boolean tryConsume(String identity, String route, int capacity, double refillPerSecond) {
        String key = "ratelimit:%s:%s".formatted(route, identity);
        long now = Instant.now().getEpochSecond();

        try {
            Long result = stringRedisTemplate.execute(
                    tokenBucketScript,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(refillPerSecond),
                    String.valueOf(now)
            );
            return result != null && result == 1L;
        } catch (Exception ex) {
            // Fail-open: If Redis is temporarily unavailable, allow the request to prevent false-positive outages
            log.warn("Redis unavailable during rate limit check for key {}. Failing open. Error: {}", key, ex.getMessage());
            return true;
        }
    }
}
