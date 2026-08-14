package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.dto.ShortenRequest;
import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlShortenerService urlShortenerService;

    @PostMapping
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request, HttpServletRequest servletRequest) {
        String baseUrl = servletRequest.getScheme() + "://" + servletRequest.getServerName()
                + (servletRequest.getServerPort() == 80 || servletRequest.getServerPort() == 443 ? "" : ":" + servletRequest.getServerPort());

        ShortenResponse response = urlShortenerService.shortenUrl(request, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
