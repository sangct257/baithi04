package ra.demo.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ra.demo.dto.response.ErrorResponse;
import ra.demo.exception.BadRequestException;
import ra.demo.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Bắt lỗi BadRequestException (Lỗi không đủ tiền ví, sai phương thức thanh toán...) -> Trả 400
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse<String>> handleBadRequestException(BadRequestException ex,HttpServletRequest request) {
        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Bắt lỗi ResourceNotFoundException (Không tìm thấy ví, tài xế...) -> Trả 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse<String>> handleResourceNotFoundException(ResourceNotFoundException ex,HttpServletRequest request) {
        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

    }

    // 1. Xử lý Lỗi Validation DTO (@Valid trên Request Body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse<Map<String, String>> response = ErrorResponse.<Map<String, String>>builder()
                .timestamp(LocalDateTime.now())
                .data(errors)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. Xử lý Lỗi Hệ thống chưa lường trước (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<String>> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        // Tùy chọn: log.error("Lỗi hệ thống: ", ex);
        log.error("Lỗi hệ thống không xác định tại URI {}: ", request.getRequestURI(), ex);

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data("Hệ thống gặp sự cố kỹ thuật!")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
