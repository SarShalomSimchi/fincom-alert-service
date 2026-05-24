package com.fincom.alerts.api;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fincom.alerts.api.GlobalExceptionHandler.ErrorResponse;
import com.fincom.alerts.exception.AlertAlreadyDecidedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void givenAlertAlreadyDecidedException_whenHandled_thenReturnsConflict() {
        AlertAlreadyDecidedException ex = new AlertAlreadyDecidedException("a1");

        ResponseEntity<ErrorResponse> response = handler.handleAlreadyDecided(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCodes.ALREADY_DECIDED, response.getBody().error());
        assertEquals(ex.getMessage(), response.getBody().message());
    }
}
