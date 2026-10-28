package ra.demo.advice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ra.demo.dto.response.ErrorResponse;
import ra.demo.exception.BadRequestException;
import ra.demo.exception.ConflictException;
import ra.demo.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Bắt lỗi Validation Request Body (@Valid DTO) -> Trả về 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse<Map<String, String>> response = ErrorResponse.<Map<String, String>>builder()
                .timestamp(LocalDateTime.now())
                .data(errors)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Bắt lỗi BadRequestException (Lỗi cú pháp/yêu cầu không hợp lệ) -> Trả về 400 Bad Request
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse<String>> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. Bắt lỗi ConflictException (Trùng lặp dữ liệu do logic Service check) -> Trả về 409 Conflict
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse<String>> handleConflictException(
            ConflictException ex, HttpServletRequest request) {

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 4. Bắt lỗi DataIntegrityViolationException (Xung đột dữ liệu tầng Database/Unique Constraint) -> Trả về 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse<String>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Vi phạm ràng buộc dữ liệu tại URI {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc hệ thống!";

        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("driver_license_number")) {
                message = "Số giấy phép lái xe đã tồn tại trên hệ thống!";
            } else if (ex.getMessage().contains("identity_card")) {
                message = "Số CCCD/CMND đã tồn tại trên hệ thống!";
            } else if (ex.getMessage().contains("license_plate")) {
                message = "Biển số xe đã được đăng ký!";
            }
        }

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 5. Bắt lỗi IllegalArgumentException (Tham số truyền vào không hợp lệ) -> Trả về 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse<String>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 6. Bắt lỗi ResourceNotFoundException (Không tìm thấy dữ liệu) -> Trả về 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse<String>> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 7. Bắt lỗi Tài khoản bị khóa -> Trả về 401 Unauthorized
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse<String>> handleLockedException(
            LockedException ex, HttpServletRequest request) {

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin!")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 8. Bắt lỗi Tài khoản bị vô hiệu hóa -> Trả về 401 Unauthorized
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse<String>> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data("Tài khoản của bạn đã bị xóa hoặc vô hiệu hóa!")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 9. Bắt lỗi hệ thống không xác định -> Trả về 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<String>> handleGeneralException(
            Exception ex, HttpServletRequest request) {

        log.error("Lỗi hệ thống không xác định tại URI {}: ", request.getRequestURI(), ex);

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .timestamp(LocalDateTime.now())
                .data("Hệ thống gặp sự cố kỹ thuật!")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}