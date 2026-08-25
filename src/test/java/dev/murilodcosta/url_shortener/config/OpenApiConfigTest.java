package dev.murilodcosta.url_shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    @DisplayName("Should configure custom OpenAPI bean with expected title, version and server metadata")
    void shouldConfigureCustomOpenApiBean() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("High-Performance URL Shortener API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("Murilo D. Costa", openAPI.getInfo().getContact().getName());
        assertNotNull(openAPI.getInfo().getLicense());
        assertEquals("MIT License", openAPI.getInfo().getLicense().getName());
        assertFalse(openAPI.getServers().isEmpty());
        assertEquals("http://localhost:8080", openAPI.getServers().getFirst().getUrl());
    }
}
