package dev.murilodcosta.url_shortener.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UrlMappingJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Test saving UrlMapping and generating ID")
    void testSaveUrlMappingAndGenerateId() {
        UrlMapping urlMapping = UrlMapping.builder()
                .shortCode("abc123")
                .longUrl("https://example.com")
                .createdAt(java.time.LocalDateTime.now())
                .build();

        UrlMapping savedUrlMapping = entityManager.persistAndFlush(urlMapping);

        assertNotNull(savedUrlMapping.getId(), "Expected ID to be generated after saving");
        assertEquals("abc123", savedUrlMapping.getShortCode(), "Expected shortCode to match");
        assertEquals("https://example.com", savedUrlMapping.getLongUrl(), "Expected longUrl to match");
    }

}