package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService urlShortenerService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToLongUrl(@PathVariable String shortCode) {
        String longUrl = urlShortenerService.resolveUrl(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
