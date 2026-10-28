package ra.demo.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import ra.demo.config.RouteValidator;
import ra.demo.dto.ErrorResponse;
import ra.demo.utils.JwtUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {
    private final JwtUtils jwtUtils;
    private final RouteValidator routeValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 0. Bỏ qua kiểm tra Authorization đối với HTTP OPTIONS (CORS Preflight request)
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 1. Nếu là API cần bảo mật
        if (routeValidator.isSecured.test(request)) {
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || authHeader.isEmpty()) {
                log.error("Thiếu header Authorization cho API: {}", request.getURI().getPath());
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Thiếu tiêu đề Authorization");
            }

            if (!authHeader.startsWith("Bearer ")) {
                log.error("Token không đúng định dạng Bearer: {}", authHeader);
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Không đúng định dạng chuỗi");
            }

            String token = authHeader.substring(7);

            // 2. Validate Token
            if (!jwtUtils.validateToken(token)) {
                log.error("Token JWT không hợp lệ hoặc đã hết hạn");
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Token JWT không hợp lệ hoặc đã hết hạn");
            }

            // 3. Extract Claims
            Long userId = jwtUtils.extractUserId(token);
            String phoneNumber = jwtUtils.extractPhoneNumber(token);
            List<String> roles = jwtUtils.extractRoles(token);

            // Apply authorization checks (Phân quyền)
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            log.info("Request: {} {} | UserId: {} | SĐT: {} | Roles: {}", method, path, userId, phoneNumber, roles);

            // 4. Phân quyền (Authorization)
            if (!routeValidator.isAuthorized(path, method, roles)) {
                log.warn("Người dùng UserId: {} | SĐT: {} | Roles: {} không được phép gọi API {} {}", userId , phoneNumber,roles, method, path);
                return onError(exchange, HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập API !"); // 403
            }


            // 5. Enrich Header & Chuyển tiếp xuống Service con
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Auth-User-Id", userId != null ? String.valueOf(userId) : "")
                    .header("X-Auth-Phone", phoneNumber != null ? phoneNumber : "")
                    .header("X-Auth-Roles", roles != null ? String.join(",", roles) : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // 2. Nếu là Public API -> Bỏ qua
        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorDetail = Map.of(
                "status", httpStatus.value(),
                "error", httpStatus.getReasonPhrase(),
                "message", message
        );

        ErrorResponse<Map<String, Object>> errorResponse = ErrorResponse.<Map<String, Object>>builder()
                .timestamp(LocalDateTime.now())
                .data(errorDetail)
                .path(exchange.getRequest().getURI().getPath())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            bytes = "{}".getBytes();
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
