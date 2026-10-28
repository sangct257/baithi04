package ra.demo.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.ErrorResponse;
import ra.demo.exception.AccessDeniedException;
import ra.demo.exception.ResourceNotFoundException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse<String>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("[RESOURCE NOT FOUND] {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse<String>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[ACCESS DENIED] {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse<String>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("[BAD REQUEST] {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<String>> handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Lỗi hệ thống tại URI {}: ", request.getRequestURI(), ex);
        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
