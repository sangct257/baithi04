package ra.demo.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import ra.demo.dto.response.ErrorResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // Cấu hình Header trả về JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // Mã 403


        Map<String, Object> errorDetail = Map.of(
                "status", HttpStatus.FORBIDDEN.value(),
                "error", "Forbidden",
                "message", "Truy cập bị từ chối: Bạn không có quyền thực hiện thao tác này!"
        );

        ErrorResponse<Map<String, Object>> errorResponse = ErrorResponse.<Map<String, Object>>builder()
                .timestamp(LocalDateTime.now())
                .data(errorDetail)
                .path(request.getRequestURI())
                .build();

        OBJECT_MAPPER.writeValue(response.getOutputStream(), errorResponse);
    }
}
