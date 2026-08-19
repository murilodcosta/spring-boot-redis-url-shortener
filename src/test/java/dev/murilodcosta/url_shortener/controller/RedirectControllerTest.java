package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.exception.UrlExpiredException;
import dev.murilodcosta.url_shortener.exception.UrlNotFoundException;
import dev.murilodcosta.url_shortener.service.RateLimiterService;
import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlShortenerService urlShortenerService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        when(rateLimiterService.tryConsume(any(), any(), anyInt(), anyDouble())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /{shortCode} should return 302 Found with Location header when shortCode exists")
    void shouldReturn302FoundWithLocationHeader() throws Exception {
        when(urlShortenerService.resolveUrl("w7e")).thenReturn("https://google.com");

        mockMvc.perform(get("/w7e"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    @DisplayName("GET /{shortCode} should return 404 Not Found when shortCode does not exist")
    void shouldReturn404NotFoundWhenShortCodeDoesNotExist() throws Exception {
        when(urlShortenerService.resolveUrl("invalid")).thenThrow(new UrlNotFoundException("invalid"));

        mockMvc.perform(get("/invalid"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("URL not found for short code: invalid"));
    }

    @Test
    @DisplayName("GET /{shortCode} should return 410 Gone when shortCode has expired")
    void shouldReturn410GoneWhenShortCodeHasExpired() throws Exception {
        when(urlShortenerService.resolveUrl("expired123")).thenThrow(new UrlExpiredException("expired123"));

        mockMvc.perform(get("/expired123"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.error").value("Gone"))
                .andExpect(jsonPath("$.message").value("The shortened URL with code 'expired123' has expired"));
    }
}
