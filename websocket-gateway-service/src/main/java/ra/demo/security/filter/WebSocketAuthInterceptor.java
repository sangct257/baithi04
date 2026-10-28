package ra.demo.security.filter;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ra.demo.security.utils.JwtUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 1. Đọc Header từ Gateway (X-Auth-User-Id)
            String userId = httpRequest.getHeader("X-Auth-User-Id");

            // 2. Dự phòng: Lấy qua Query Parameter trên URL (?userId=3 hoặc ?token=xxx)
            if (!StringUtils.hasText(userId)) {
                userId = httpRequest.getParameter("userId");
            }

            // 3. Dự phòng: Đọc Token từ URL query (?token=eyJhbGci...)
            if (!StringUtils.hasText(userId)) {
                String token = httpRequest.getParameter("token");
                if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                    userId = String.valueOf(jwtUtils.extractUserId(token));
                }
            }

            // Lưu userId vào Session Attributes của WebSocket để Controller sử dụng
            if (StringUtils.hasText(userId)) {
                attributes.put("userId", userId);
                return true;
            }
        }

        // Trả về true nếu muốn bypass kiểm tra cho môi trường dev test
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}