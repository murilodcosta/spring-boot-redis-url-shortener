package dev.murilodcosta.url_shortener.config;

import dev.murilodcosta.url_shortener.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        boolean allowed;
        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/urls")) {
            // Creation route: more restrictive (10 requests per minute)
            allowed = rateLimiterService.tryConsume(clientIp, "shorten", 10, 10.0 / 60.0);
        } else if ("GET".equalsIgnoreCase(method) && !uri.startsWith("/actuator") && !uri.startsWith("/api")) {
            // Redirect route: high throughput (100 requests per second)
            allowed = rateLimiterService.tryConsume(clientIp, "redirect", 100, 100.0);
        } else {
            allowed = true;
        }

        if (!allowed) {
            log.warn("Rate limit exceeded for IP: {} on [{}] {}", clientIp, method, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {
                        "status": 429,
                        "error": "Too Many Requests",
                        "message": "Rate limit exceeded. Please try again later.",
                        "timestamp": "%s"
                    }
                    """.formatted(LocalDateTime.now()));
            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
