package com.termostato.web;

import com.termostato.config.ConfigurationPersistenceException;
import com.termostato.web.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

    private final Clock clock;

    public RestExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> badRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> invalidBody(Exception exception) {
        String message = exception.getMessage();
        if (exception instanceof MethodArgumentNotValidException validation) {
            message = validation.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        }
        return response(HttpStatus.BAD_REQUEST, message == null ? "Payload non valido" : message);
    }

    @ExceptionHandler(ConfigurationPersistenceException.class)
    public ResponseEntity<ApiError> persistenceFailure(ConfigurationPersistenceException exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> internalFailure(Exception exception) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno del server");
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(clock), status.value(), status.getReasonPhrase(), message));
    }
}
