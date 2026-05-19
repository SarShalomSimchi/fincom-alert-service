package com.fincom.alerts.api;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fincom.alerts.exception.AlertAlreadyDecidedException;
import com.fincom.alerts.exception.AlertNotFoundException;
import com.fincom.alerts.exception.InvalidTransitionException;
import com.fincom.alerts.exception.MissingTenantException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String error, String message) {}

    @ExceptionHandler(MissingTenantException.class)
    public ResponseEntity<ErrorResponse> handleMissingTenant(MissingTenantException ex) {
        return error(HttpStatus.BAD_REQUEST, ErrorCodes.MISSING_TENANT, ex.getMessage());
    }

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AlertNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidTransitionException ex) {
        return error(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_TRANSITION, ex.getMessage());
    }

    @ExceptionHandler(AlertAlreadyDecidedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyDecided(AlertAlreadyDecidedException ex) {
        return error(HttpStatus.CONFLICT, ErrorCodes.ALREADY_DECIDED, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, message);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, message);
    }
    
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(code, message));
    }
}