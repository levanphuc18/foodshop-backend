package com.foodshop.exception;

import com.foodshop.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException ex) {
        log.warn("Disabled account access attempt: {}", ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>(
                GlobalCode.ACCOUNT_DISABLED.getCode(),
                GlobalCode.ACCOUNT_DISABLED.getMessage(),
                null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>(
                GlobalCode.UNAUTHORIZED.getCode(),
                "Invalid username or password.",
                null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(GlobalException ex) {
        log.warn("Business exception code={} message={}", ex.getCode(), ex.getMessage());
        ApiResponse<Void> response = new ApiResponse<>(ex.getCode(), ex.getMessage(), null);
        HttpStatus status = mapCodeToStatus(ex.getCode());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", message);
        ApiResponse<Void> response = new ApiResponse<>(GlobalCode.BAD_REQUEST.getCode(), message, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiResponse<Void> response = new ApiResponse<>(
                GlobalCode.INTERNAL_SERVER_ERROR.getCode(),
                GlobalCode.INTERNAL_SERVER_ERROR.getMessage(),
                null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus mapCodeToStatus(int code) {
        return switch (code) {
            case 0 -> HttpStatus.OK;
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 4291 -> HttpStatus.TOO_MANY_REQUESTS;
            case 500 -> HttpStatus.INTERNAL_SERVER_ERROR;
            case 1001, 1003, 2001, 2003, 3000, 3001, 3003, 3004, 3005, 4001, 4002, 5000, 5001, 5002, 5003, 6002, 6003, 7002 -> HttpStatus.BAD_REQUEST;
            case 2002, 2004, 3002, 6001, 7001, 8001 -> HttpStatus.NOT_FOUND;
            case 4011, 4012 -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
