package com.eyarko.ecom.exception;

import com.eyarko.ecom.dto.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .status("error")
            .message(ex.getReason())
            .build();
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
            .status("error")
            .message("Validation failed")
            .data(errors)
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(HttpMessageNotReadableException ex) {
        logger.warn("Bad request body: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .status("error")
            .message("Invalid request body")
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = resolveIntegrityMessage(ex);
        logger.warn("Data integrity violation: {}", message);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .status("error")
            .message(message)
            .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        logger.warn("No resource found: {}", ex.getResourcePath());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .status("error")
            .message("Not found")
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        logger.error("Unhandled exception", ex);
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .status("error")
            .message("Unexpected error")
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String resolveIntegrityMessage(DataIntegrityViolationException ex) {
        String details = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        String lower = details == null ? "" : details.toLowerCase();
        if (lower.contains("order_items") || lower.contains("order_item")) {
            return "This item cannot be deleted because it is linked to orders";
        }
        if (lower.contains("cart_items") || lower.contains("cart_item")) {
            return "This item cannot be deleted because it is linked to carts";
        }
        if (lower.contains("products") && (lower.contains("category") || lower.contains("categories"))) {
            return "This category cannot be deleted because it has products";
        }
        if (lower.contains("inventory")) {
            return "This product cannot be deleted because inventory exists";
        }
        if (lower.contains("reviews")) {
            return "This product cannot be deleted because it has reviews";
        }
        if (lower.contains("orders")) {
            return "This user cannot be deleted because it has orders";
        }
        if (lower.contains("carts")) {
            return "This user cannot be deleted because it has an active cart";
        }
        return "This resource cannot be deleted because it is linked to other records";
    }
}


