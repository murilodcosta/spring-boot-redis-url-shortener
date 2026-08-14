package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.exception.UrlNotFoundException;
import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
