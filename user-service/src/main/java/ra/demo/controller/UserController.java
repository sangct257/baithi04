package ra.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ra.demo.constants.AccountStatus;
import ra.demo.dto.response.ApiResponse;
import ra.demo.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Admin đổi trạng thái tài khoản
    @PatchMapping("/admin/{userId}/status")
    public ResponseEntity<ApiResponse<String>> adminChangeAccountStatus(
            Authentication authentication,
            @PathVariable("userId") Long targetUserId,
            @RequestParam AccountStatus status) {

        Long adminUserId = (Long) authentication.getPrincipal();

        userService.adminChangeAccountStatus(adminUserId, targetUserId, status);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cập nhật trạng thái tài khoản thành công!",
                "Trạng thái mới: " + status,
                null,
                HttpStatus.OK
        ));
    }

    // Khách hàng tự vô hiệu hóa tài khoản (Gateway/Filter sẽ truyền phoneNumber qua Header)
    @DeleteMapping("/customer/customer-deactivate")
    public ResponseEntity<ApiResponse<String>> customerSelfDisableAccount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        userService.customerChangeAccountStatus(userId);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Vô hiệu hóa tài khoản Khách hàng thành công!",
                "Tài khoản của bạn đã được chuyển sang trạng thái CUSTOMER_DELETED",
                null,
                HttpStatus.OK
        ));
    }

    // Tài xế tự vô hiệu hóa tài khoản (Gateway/Filter sẽ truyền phoneNumber qua Header)
    @DeleteMapping("/driver/driver-deactivate")
    public ResponseEntity<ApiResponse<String>> driverSelfDisableAccount(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        userService.driverChangeAccountStatus(userId);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Vô hiệu hóa tài khoản Tài xế thành công!",
                "Tài khoản của bạn đã được chuyển sang trạng thái DRIVER_DELETED",
                null,
                HttpStatus.OK
        ));
    }
}
