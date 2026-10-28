package ra.demo.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    public static final List<String> openApiEndpoints = List.of(
            "/user-service/api/v1/auth/login",
            "/user-service/api/v1/auth/driver/send-otp",
            "/user-service/api/v1/auth/login/driver",
            "/user-service/api/v1/auth/register/customer",
            "/user-service/api/v1/auth/register/driver",
            "/user-service/api/v1/auth/refresh-token",
            "/user-service/api/v1/auth/logout",
            "/location-tracking-service/api/v1/location/nearby",
            "/eureka");

    public Predicate<ServerHttpRequest> isSecured = request -> openApiEndpoints
            .stream()
            .noneMatch(uri -> request.getURI().getPath().contains(uri));

    public boolean isAuthorized(String path, String method, List<String> roles) {
        // MẶC ĐỊNH: ROLE_ADMIN có FULL quyền truy cập tất cả API
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            return true;
        }

        // 2. Nếu không phải Admin, kiểm tra theo các luật cụ thể bên dưới
        for (RouteRule rule : AUTHORIZATION_RULES) {
            if (rule.matches(path, method)) {
                if (!rule.isAuthorized(roles)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final List<RouteRule> AUTHORIZATION_RULES = List.of(
            // ==================== 1. USER SERVICE ====================
            new RouteRule("/user-service/api/v1/users/customer/customer-deactivate", List.of("DELETE"), List.of("ROLE_CUSTOMER")),
            new RouteRule("/user-service/api/v1/users/driver/driver-deactivate", List.of("DELETE"), List.of("ROLE_DRIVER")),
            new RouteRule("/user-service/api/v1/driver/profile", List.of("POST", "GET"), List.of("ROLE_DRIVER")),
            new RouteRule("/user-service/api/v1/driver/status", List.of("PATCH", "PUT"), List.of("ROLE_DRIVER")),

            // ==================== 2. LOCATION TELEMETRY SERVICE ====================
            // 2.1. Tài xế push GPS định kỳ
            new RouteRule("/location-tracking-service/api/v1/location/driver", List.of("POST"), List.of("ROLE_DRIVER")),

            // 2.2. Tìm tài xế xung quanh (Dành cho Khách hàng & Admin, cũng như Service-to-Service qua Gateway)
            new RouteRule("/location-tracking-service/api/v1/location/nearby", List.of("GET"), List.of("ROLE_CUSTOMER", "ROLE_ADMIN")),

            // 2.3. Lấy vị trí cụ thể / Xóa tài xế khỏi danh sách rảnh
            new RouteRule("/location-tracking-service/api/v1/location/driver/*", List.of("GET", "DELETE"), List.of("ROLE_DRIVER", "ROLE_CUSTOMER", "ROLE_ADMIN")),

            // ==================== 3. TRIP DISPATCH SERVICE ====================
            // 3.1. KHÁCH HÀNG (CUSTOMER)
            // Đặt chuyến đi mới
            new RouteRule("/trip-dispatch-service/api/v1/trips", List.of("POST"), List.of("ROLE_CUSTOMER")),
            // Xem lịch sử chuyến đi của khách
            new RouteRule("/trip-dispatch-service/api/v1/trips/customer/history", List.of("GET"), List.of("ROLE_CUSTOMER")),
            // Xem chi tiết 1 chuyến đi của khách
            new RouteRule("/trip-dispatch-service/api/v1/trips/*", List.of("GET"), List.of("ROLE_CUSTOMER")),
            // Khách hàng hủy chuyến đi
            new RouteRule("/trip-dispatch-service/api/v1/trips/*/customer-cancel", List.of("PUT"), List.of("ROLE_CUSTOMER")),

            // 3.2. TÀI XẾ (DRIVER)
            // Xem lịch sử chuyến đi của tài xế
            new RouteRule("/trip-dispatch-service/api/v1/trips/driver/history", List.of("GET"), List.of("ROLE_DRIVER")),
            // Tài xế nhận chuyến đi (Accept Trip)
            new RouteRule("/trip-dispatch-service/api/v1/trips/*/accept", List.of("PUT"), List.of("ROLE_DRIVER")),
            // Tài xế cập nhật tiến trình chuyến (ARRIVED -> IN_PROGRESS -> COMPLETED)
            new RouteRule("/trip-dispatch-service/api/v1/trips/*/status", List.of("PUT"), List.of("ROLE_DRIVER")),
            // Tài xế hủy chuyến
            new RouteRule("/trip-dispatch-service/api/v1/trips/*/driver-cancel", List.of("PUT"), List.of("ROLE_DRIVER")),

            // ==================== 4. PRICING SURGE SERVICE ====================
            // Khách hàng & Admin được phép gọi API tính giá cước chuyến đi
            new RouteRule("/pricing-surge-service/api/v1/pricing/calculate", List.of("POST"), List.of("ROLE_CUSTOMER"))
    );
}
