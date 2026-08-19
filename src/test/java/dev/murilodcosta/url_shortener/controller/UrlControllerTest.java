package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.service.RateLimiterService;
import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlController.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlShortenerService urlShortenerService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        Counter counter = mock(Counter.class);
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
        lenient().when(rateLimiterService.tryConsume(any(), any(), anyInt(), anyDouble())).thenReturn(true);
    }

    @Test
    @DisplayName("POST /api/urls should return 201 Created with JSON response when request is valid")
    void shouldReturn201CreatedWhenRequestIsValid() throws Exception {
        ShortenResponse mockResponse = new ShortenResponse(
                "w7e",
                "http://localhost:8080/w7e",
                "https://google.com",
                LocalDateTime.now(),
                null
        );

        when(urlShortenerService.shortenUrl(any(), any())).thenReturn(mockResponse);

        String requestBody = """
                {
                    "url": "https://google.com",
                    "expiresInMinutes": 60
                }
                """;

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("w7e"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/w7e"))
                .andExpect(jsonPath("$.longUrl").value("https://google.com"));
    }

    @Test
    @DisplayName("POST /api/urls should return 400 Bad Request when url is invalid")
    void shouldReturn400BadRequestWhenUrlIsInvalid() throws Exception {
        String invalidRequestBody = """
                {
                    "url": "invalid-url-format"
                }
                """;

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation error in the provided fields"))
                .andExpect(jsonPath("$.validationErrors.url").exists());
    }
}
