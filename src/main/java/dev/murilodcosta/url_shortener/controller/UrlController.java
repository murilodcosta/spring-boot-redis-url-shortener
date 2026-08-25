package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.dto.ShortenRequest;
import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.exception.ErrorResponse;
import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "URLs", description = "Endpoints for creating and managing shortened URLs")
@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlShortenerService urlShortenerService;

    @Operation(
            summary = "Create shortened URL",
            description = "Encodes original long URL into a deterministic Base62 short code, persists in PostgreSQL, and warms up Redis cache."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "URL successfully shortened",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ShortenResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid URL or input validation error",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded (Maximum 10 requests per minute per IP)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request, HttpServletRequest servletRequest) {
        String baseUrl = servletRequest.getScheme() + "://" + servletRequest.getServerName()
                + (servletRequest.getServerPort() == 80 || servletRequest.getServerPort() == 443 ? "" : ":" + servletRequest.getServerPort());

        ShortenResponse response = urlShortenerService.shortenUrl(request, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
