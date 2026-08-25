package dev.murilodcosta.url_shortener.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle UrlNotFoundException and return 404 Not Found")
    void shouldHandleUrlNotFoundException() {
        UrlNotFoundException ex = new UrlNotFoundException("abc1234");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUrlNotFound(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("URL not found for short code: abc1234", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should handle UrlExpiredException and return 410 Gone")
    void shouldHandleUrlExpiredException() {
        UrlExpiredException ex = new UrlExpiredException("expiredCode");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUrlExpired(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.GONE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(410, response.getBody().status());
        assertEquals("Gone", response.getBody().error());
        assertEquals("The shortened URL with code 'expiredCode' has expired", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should handle NoResourceFoundException and return 404 Not Found")
    void shouldHandleNoResourceFoundException() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/static/file.css", "Resource not found");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNoResourceFound(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should handle generic Exception and return 500 Internal Server Error")
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("Database timeout");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Internal Server Error", response.getBody().error());
        assertTrue(response.getBody().message().contains("Database timeout"));
        assertNotNull(response.getBody().timestamp());
    }
}
