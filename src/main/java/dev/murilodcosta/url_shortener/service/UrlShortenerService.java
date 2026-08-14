package dev.murilodcosta.url_shortener.service;

import dev.murilodcosta.url_shortener.dto.ShortenRequest;
import dev.murilodcosta.url_shortener.dto.ShortenResponse;
import dev.murilodcosta.url_shortener.exception.UrlExpiredException;
import dev.murilodcosta.url_shortener.exception.UrlNotFoundException;
import dev.murilodcosta.url_shortener.model.UrlMapping;
import dev.murilodcosta.url_shortener.repository.UrlMappingRepository;
import dev.murilodcosta.url_shortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, String baseUrl) {
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = request.expiresInMinutes() != null
                ? createdAt.plusMinutes(request.expiresInMinutes())
                : null;

        UrlMapping urlMapping = UrlMapping.builder()
                .longUrl(request.url())
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();

        // 1. Save the UrlMapping to get the generated ID
        UrlMapping saved = urlMappingRepository.save(urlMapping);

        // 2. Convert the generated ID to a Base62 short code
        String shortCode = Base62Encoder.encode(saved.getId());
        saved.setShortCode(shortCode);

        // 3. Update the UrlMapping with the short code
        urlMappingRepository.save(saved);

        String shortUrl = baseUrl + "/" + shortCode;

        return new ShortenResponse(
                shortCode,
                shortUrl,
                saved.getLongUrl(),
                saved.getCreatedAt(),
                saved.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public String resolveUrl(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (urlMapping.isExpired()) {
            throw new UrlExpiredException(shortCode);
        }

        return urlMapping.getLongUrl();
    }
}
