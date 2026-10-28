package ra.demo.config;

import org.springframework.util.AntPathMatcher;

import java.util.List;

public class RouteRule {
    private final String pathPattern;
    private final List<String> methods;
    private final List<String> requiredRoles;

    /**
     * Khởi tạo một quy tắc phân quyền cho route.
     *
     * @param pathPattern   Chuỗi ký tự hoặc pattern của đường dẫn cần kiểm tra (ví
     *                      dụ: "/api/v1/categories")
     * @param methods       Danh sách các phương thức HTTP áp dụng quy tắc này (ví
     *                      dụ: POST, PUT, DELETE). Nếu rỗng nghĩa là áp dụng cho
     *                      tất cả các phương thức.
     * @param requiredRoles Danh sách các vai trò (roles) yêu cầu để truy cập. Nếu
     *                      rỗng nghĩa là không yêu cầu vai trò cụ thể nào.
     */
    public RouteRule(String pathPattern, List<String> methods, List<String> requiredRoles) {
        this.pathPattern = pathPattern;
        this.methods = methods;
        this.requiredRoles = requiredRoles;
    }

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Kiểm tra xem đường dẫn và phương thức HTTP của request hiện tại có khớp với
     * quy tắc này hay không.
     *
     * @param path   Đường dẫn của request thực tế gửi lên
     * @param method Phương thức HTTP (GET, POST, PUT, DELETE,...) của request thực
     *               tế
     * @return true nếu khớp với quy tắc này, ngược lại trả về false
     */
    public boolean matches(String path, String method) {
        boolean pathMatches = pathMatcher.match(this.pathPattern, path);

        boolean methodMatches = this.methods.isEmpty()
                || (method != null && this.methods.contains(method.toUpperCase()));

        return pathMatches && methodMatches;
    }

    /**
     * Xác thực xem danh sách các vai trò (roles) của người dùng hiện tại có đủ
     * quyền truy cập theo quy tắc này không.
     *
     * @param userRoles Danh sách vai trò của người dùng được trích xuất từ JWT
     *                  token
     * @return true nếu người dùng có ít nhất một trong các vai trò yêu cầu, hoặc
     *         quy tắc không yêu cầu vai trò cụ thể. Ngược lại trả về false.
     */
    public boolean isAuthorized(List<String> userRoles) {
        if (requiredRoles.isEmpty()) {
            return true;
        }
        if (userRoles == null) {
            return false;
        }
        return userRoles.stream().anyMatch(requiredRoles::contains);
    }
}
