package ra.demo.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    /**
     * Hàm tiện ích giúp lấy UserId của người dùng hiện tại ở bất kỳ đâu trong lớp Service.
     * @return Long userId
     */
    public static Long getCurrentUserId() {
        // Lấy đối tượng Authentication từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem User đã được xác thực chưa
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Chưa đăng nhập hoặc thiếu thông tin người dùng từ Gateway!");
        }

        // Vì ở Filter ta đã lưu 'userId' vào vị trí Principal, nên ở đây chỉ việc ép kiểu sang Long
        return (Long) authentication.getPrincipal();
    }
}