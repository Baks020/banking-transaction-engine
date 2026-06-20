package com.example.banking_transaction_engine.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepts request body validation failures and maps them to standard error formats.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();

        // Safely extract field-level errors using core Spring binding abstractions
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = (error instanceof FieldError fieldError) ? fieldError.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("errorCode", "VALIDATION_FAILED");
        responseBody.put("message", "Validation failed for incoming request properties");
        responseBody.put("validationErrors", validationErrors);

        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    /**
     * Catch-all business exception interceptor (e.g., duplicate entries or business logic violations).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> responseBody = Map.of(
                "errorCode", "BUSINESS_RULE_VIOLATION",
                "message", ex.getMessage()
        );
        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }
}
