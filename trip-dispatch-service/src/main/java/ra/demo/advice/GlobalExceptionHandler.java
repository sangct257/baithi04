package ra.demo.advice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ra.demo.dto.response.ErrorResponse;
import ra.demo.exception.TripException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. Xử lý Lỗi Nghiệp vụ do chúng ta tự ném ra (TripException)
    @ExceptionHandler(TripException.class)
    public ResponseEntity<ErrorResponse<String>> handleTripException(TripException ex, HttpServletRequest request) {
        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Xử lý Lỗi Validation DTO (@Valid trên Request Body)
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

    // 3. Xử lý Lỗi từ các Microservice khác qua OpenFeign
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ErrorResponse<String>> handleFeignException(
            feign.FeignException ex, HttpServletRequest request) {

        String message = extractErrorMessage(ex);

        // Log rõ ràng ra Console của Trip Service khi tạo chuyến thất bại do bảng giá / pricing
        log.error("[TRIP ERROR] Tạo chuyến đi thất bại: {}", message);

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private String extractErrorMessage(feign.FeignException ex) {
        try {
            String responseBody = ex.contentUTF8();
            if (responseBody != null && !responseBody.isBlank()) {
                JsonNode root = objectMapper.readTree(responseBody);

                // 1. Kiểm tra trường 'data' (Nếu là ApiResponse tiêu chuẩn)
                if (root.has("data") && !root.get("data").isNull() && !root.get("data").asText().isBlank()) {
                    return root.get("data").asText();
                }
                // 2. Kiểm tra trường 'message'
                if (root.has("message") && !root.get("message").isNull() && !root.get("message").asText().isBlank()) {
                    return root.get("message").asText();
                }
            }
        } catch (Exception ignored) {
            log.error("[PARSE ERROR] Không thể parse JSON từ Feign Exception: {}", ex.getMessage());
        }
        return String.format("Lỗi gọi Pricing Service (HTTP %d): %s", ex.status(), ex.getMessage());
    }

    // 4. Xử lý Lỗi Hệ thống chưa lường trước (500 Internal Server Error)
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
