package ra.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ra.demo.dto.request.DriverProfileRegisterRequest;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.DriverProfileResponse;
import ra.demo.service.DriverService;

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    // Tạo hồ sơ tài xế
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> registerProfile(
            Authentication authentication,
            @Valid @RequestBody DriverProfileRegisterRequest request) {

        Long userId = (Long) authentication.getPrincipal();
        DriverProfileResponse response = driverService.createOrUpdateProfile(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                true,
                "Cập nhật hồ sơ tài xế và thông tin xe thành công!",
                response,
                null,
                HttpStatus.CREATED
        ));
    }

    // Thông tin hồ sơ tài xế
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> getMyProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        DriverProfileResponse response = driverService.getProfile(userId);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lấy thông tin hồ sơ thành công",
                response,
                null,
                HttpStatus.OK
        ));
    }

    // Tài xế bật/tắt trạng thái hoạt động
    @PatchMapping("/status")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> updateStatus(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        DriverProfileResponse response = driverService.changeStatus(userId);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Đã chuyển trạng thái hoạt động thành " + response.getStatus(),
                response,
                null,
                HttpStatus.OK
        ));
    }

    // ADMIN duyệt hồ sơ tài xế
    @PatchMapping("/admin/drivers/{driverId}/approve")
    public ResponseEntity<ApiResponse<DriverProfileResponse>> approveDriver(
            @PathVariable Long driverId) {

        DriverProfileResponse response = driverService.approveDriverProfile(driverId);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                Boolean.TRUE.equals(response.getIsApproved()) ? "Đã phê duyệt hồ sơ tài xế!" : "Đã hủy phê duyệt hồ sơ!",
                response,
                null,
                HttpStatus.OK
        ));
    }
}