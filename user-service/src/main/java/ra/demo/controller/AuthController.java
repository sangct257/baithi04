package ra.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ra.demo.dto.request.*;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.JwtResponse;
import ra.demo.entity.User;
import ra.demo.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    // Khách hàng đăng ký
    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody CustomerRegisterRequest customerRegisterRequest) {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Đăng ký tài khoản khách hàng thành công",
                userService.registerCustomer(customerRegisterRequest),
                null,
                HttpStatus.CREATED
        ), HttpStatus.CREATED);
    }

    // Tài xế đăng ký
    @PostMapping("/register/driver")
    public ResponseEntity<ApiResponse<User>> registerDriver(@Valid @RequestBody DriverRegisterRequest driverRegisterRequest) {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Đăng ký tài khoản tài xế thành công",
                userService.registerDriver(driverRegisterRequest),
                null,
                HttpStatus.CREATED
        ), HttpStatus.CREATED);
    }

    // Khách hàng login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> customerLogin(@Valid @RequestBody CustomerLoginRequest customerLoginRequest) {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Đăng nhập thành công",
                userService.loginCustomer(customerLoginRequest),
                null,
                HttpStatus.OK
        ), HttpStatus.OK);
    }

    // Lấy otp
    @PostMapping("/driver/send-otp")
    public ResponseEntity<ApiResponse<String>> sendDriverOtp(@Valid @RequestBody SendOtpRequest sendOtpRequest) {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Lấy OTP thành công",
                userService.sendOtpForDriver(sendOtpRequest),
                null,
                HttpStatus.CREATED
        ), HttpStatus.CREATED);
    }

    // Tài xế login
    @PostMapping("/login/driver")
    public ResponseEntity<ApiResponse<JwtResponse>> loginDriver(@Valid @RequestBody DriverLoginRequest driverLoginRequest) {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Đăng nhập thành công",
                userService.loginDriver(driverLoginRequest),
                null,
                HttpStatus.OK
        ), HttpStatus.OK);
    }

    // Refresh token
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Đã cấp lại access token mới",
                userService.refreshToken(refreshTokenRequest.getRefreshToken()),
                null,
                HttpStatus.OK
        ), HttpStatus.OK);
    }

    // Đăng xuất tài khoản
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(Authentication authentication) {
        String phoneNumber = authentication.getName();
        userService.logout(phoneNumber);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Đăng xuất thành công!",
                "Phiên làm việc đã kết thúc.",
                null,
                HttpStatus.OK
        ));
    }

}
