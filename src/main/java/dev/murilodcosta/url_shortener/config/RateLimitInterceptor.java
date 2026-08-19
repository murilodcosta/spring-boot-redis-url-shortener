package dev.murilodcosta.url_shortener.config;

import dev.murilodcosta.url_shortener.service.RateLimiterService;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        boolean allowed;
        String routeName;

        if ("POST".equalsIgnoreCase(method) && uri.startsWith("/api/urls")) {
            routeName = "shorten";
            allowed = rateLimiterService.tryConsume(clientIp, routeName, 10, 10.0 / 60.0);
        } else if ("GET".equalsIgnoreCase(method) && !uri.startsWith("/actuator") && !uri.startsWith("/api")) {
            routeName = "redirect";
            allowed = rateLimiterService.tryConsume(clientIp, routeName, 100, 100.0);
        } else {
            return true;
        }

        if (!allowed) {
            meterRegistry.counter("ratelimit.requests", "route", routeName, "result", "rejected").increment();
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

        meterRegistry.counter("ratelimit.requests", "route", routeName, "result", "allowed").increment();
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
