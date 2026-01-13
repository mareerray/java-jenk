package com.buyone.productservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;
import com.buyone.productservice.response.ErrorResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class GlobalExceptionHandlerTests {

    private GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    public void testBuildErrorMethod() {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Test error message";
        String path = "/api/test";

        ErrorResponse response = handler.buildError(status, message, path);

        assertNotNull(response);
        assertEquals(status.value(), response.getStatus());
        assertEquals(message, response.getMessage());
        assertEquals(path, response.getPath());
    }

    @Test
    public void testBuildErrorWithDifferentStatus() {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = "Resource not found";
        String path = "/api/products/999";

        ErrorResponse response = handler.buildError(status, message, path);

        assertNotNull(response);
        assertEquals(404, response.getStatus());
    }
}
