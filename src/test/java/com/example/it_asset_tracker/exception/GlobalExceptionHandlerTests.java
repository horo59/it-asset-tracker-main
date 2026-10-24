
package com.example.it_asset_tracker.exception;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;



import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();

        request = new MockHttpServletRequest();
        request.setRequestURI("/api/assets/99999");
    }

    @Test
    void handlesNotFoundException() {
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Asset not found");

        ResponseEntity<Map<String, Object>> response =
                handler.handleResponseStatus(exception, request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Asset not found",
                response.getBody().get("message"));
        assertEquals(404, response.getBody().get("status"));
        assertEquals("/api/assets/99999",
                response.getBody().get("path"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }

    @Test
    void handlesConflictException() {
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.CONFLICT, "Duplicate serial number");

        ResponseEntity<Map<String, Object>> response =
                handler.handleResponseStatus(exception, request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Duplicate serial number",
                response.getBody().get("message"));
        assertEquals("/api/assets/99999",
                response.getBody().get("path"));
    }

    @Test
    void handlesBadRequestException() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnreadableMessage(null, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(400, response.getBody().get("status"));
        assertTrue(response.getBody().containsKey("timestamp"));
        assertEquals("/api/assets/99999",
                response.getBody().get("path"));
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnexpectedException(
                        new RuntimeException("Internal database details"),
                        request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("An unexpected error occurred",
                response.getBody().get("message"));
        assertTrue(!response.getBody().get("message")
                .toString().contains("Internal database details"));
        assertEquals("/api/assets/99999",
                response.getBody().get("path"));
    }
}
