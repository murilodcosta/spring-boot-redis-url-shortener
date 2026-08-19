package dev.murilodcosta.url_shortener.config;

import dev.murilodcosta.url_shortener.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RateLimitInterceptor interceptor;

    @Test
    @DisplayName("Should allow POST /api/urls request when rate limit is not exceeded")
    void shouldAllowPostRequestWhenWithinRateLimit() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/urls");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(rateLimiterService.tryConsume(eq("192.168.1.100"), eq("shorten"), eq(10), anyDouble())).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should block POST /api/urls and return 429 when rate limit is exceeded")
    void shouldBlockPostRequestAndReturn429WhenRateLimitExceeded() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/urls");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(rateLimiterService.tryConsume(eq("192.168.1.100"), eq("shorten"), eq(10), anyDouble())).thenReturn(false);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response, times(1)).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        assertTrue(stringWriter.toString().contains("429"));
        assertTrue(stringWriter.toString().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("Should extract client IP from X-Forwarded-For header when behind proxy")
    void shouldExtractClientIpFromXForwardedForHeader() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/w7e");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18");
        when(rateLimiterService.tryConsume(eq("203.0.113.195"), eq("redirect"), eq(100), eq(100.0))).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(rateLimiterService, times(1)).tryConsume(eq("203.0.113.195"), eq("redirect"), eq(100), eq(100.0));
    }
}
