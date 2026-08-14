package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
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

    @Test
    @DisplayName("Should return 201 Created when request is valid")
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
    @DisplayName("Should return 400 Bad Request when URL is invalid")
    void shouldReturn400BadRequestWhenUrlIsInvalid() throws Exception {
        String invalidRequestBody = """
                {
                    "url": "nao-e-uma-url-valida"
                }
                """;

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
