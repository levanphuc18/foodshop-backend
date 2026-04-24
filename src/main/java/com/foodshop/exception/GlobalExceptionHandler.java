package com.foodshop.exception;

import com.foodshop.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(DisabledException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                GlobalCode.ACCOUNT_DISABLED.getCode(),
                GlobalCode.ACCOUNT_DISABLED.getMessage(),
                null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                GlobalCode.UNAUTHORIZED.getCode(),
                "Invalid username or password.",
                null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles all custom business logic exceptions (GlobalException)
     */
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(GlobalException ex) {
        ApiResponse<Void> response = new ApiResponse<>(ex.getCode(), ex.getMessage(), null);
        HttpStatus status = mapCodeToStatus(ex.getCode());
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handles @Valid/@Validated bean validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ApiResponse<Void> response = new ApiResponse<>(
                GlobalCode.BAD_REQUEST.getCode(), message, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Fallback for any unhandled exception
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                GlobalCode.INTERNAL_SERVER_ERROR.getCode(),
                // dev
                "An unexpected error occurred: " + ex.getMessage(),
                // production
                // "An unexpected error occurred. Please try again later.",
                null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus mapCodeToStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
