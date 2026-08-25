package dev.murilodcosta.url_shortener.controller;

import dev.murilodcosta.url_shortener.exception.ErrorResponse;
import dev.murilodcosta.url_shortener.service.ClickTrackingService;
import dev.murilodcosta.url_shortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Redirect", description = "Public redirection endpoint")
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final ClickTrackingService clickTrackingService;

    @Operation(
            summary = "Redirect to original URL",
            description = "Resolves shortCode from Redis (Cache-Aside) or PostgreSQL, tracks click count asynchronously, and returns HTTP 302 Found with Location header."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "302",
                    description = "Redirect found. Target URL is in the Location header.",
                    headers = @Header(name = "Location", description = "Destination long URL", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "URL not found for provided shortCode",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "Shortened URL has expired",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded (Maximum 100 requests per second per IP)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "Base62 encoded short code", example = "1")
            @PathVariable String shortCode
    ) {
        String longUrl = urlShortenerService.resolveUrl(shortCode);
        clickTrackingService.registerClick(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
