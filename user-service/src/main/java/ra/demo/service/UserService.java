package ra.demo.service;

import ra.demo.constants.AccountStatus;
import ra.demo.dto.request.*;
import ra.demo.dto.response.JwtResponse;
import ra.demo.entity.User;

public interface UserService {
    // Auth Register Customer/Driver ,Login Customer/Driver, send otp for Driver, refresh token, logout
    User registerCustomer(CustomerRegisterRequest customerRegisterRequest);
    User registerDriver(DriverRegisterRequest driverRegisterRequest);
    JwtResponse loginCustomer(CustomerLoginRequest customerLoginRequest);
    JwtResponse loginDriver(DriverLoginRequest driverLoginRequest);
    String sendOtpForDriver(SendOtpRequest sendOtpRequest);
    JwtResponse refreshToken(String refreshToken);
    void logout(String phoneNumber);

    // ADMIN update user status (ACTIVE, CUSTOMER_LOCKED, DRIVER_LOCKED, CUSTOMER_DELETED, DRIVER_DELETED)
    void adminChangeAccountStatus(Long adminUserId, Long targetUserId, AccountStatus accountStatus);

    // Customer Customer_LOCKED, DRIVER_LOCKED
    void customerChangeAccountStatus(Long userId);

    // Driver CUSTOMER_DELETED, DRIVER_DELETED
    void driverChangeAccountStatus(Long userId);
}
