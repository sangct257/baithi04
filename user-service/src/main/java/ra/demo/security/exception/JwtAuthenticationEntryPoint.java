package ra.demo.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import ra.demo.dto.response.ErrorResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // Cấu hình Header trả về là JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);// Mã 401

        // Đưa thông tin chi tiết lỗi vào field data
        Map<String, Object> errorDetail = Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", "Xác thực thất bại: Token không hợp lệ hoặc đã hết hạn!"
        );

        ErrorResponse<Map<String, Object>> errorResponse = ErrorResponse.<Map<String, Object>>builder()
                .timestamp(LocalDateTime.now())
                .data(errorDetail)
                .path(request.getRequestURI())
                .build();

        OBJECT_MAPPER.writeValue(response.getOutputStream(), errorResponse);
    }
}
