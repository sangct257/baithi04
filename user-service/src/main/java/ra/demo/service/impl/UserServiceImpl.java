package ra.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ra.demo.constants.AccountStatus;
import ra.demo.constants.RoleEnums;
import ra.demo.dto.request.*;
import ra.demo.dto.response.JwtResponse;
import ra.demo.entity.RefreshToken;
import ra.demo.entity.Role;
import ra.demo.entity.User;
import ra.demo.exception.BadRequestException;
import ra.demo.exception.ResourceNotFoundException;
import ra.demo.repository.RefreshTokenRepository;
import ra.demo.repository.RoleRepository;
import ra.demo.repository.UserRepository;
import ra.demo.security.filter.JwtProvider;
import ra.demo.security.principal.CustomUserDetails;
import ra.demo.security.principal.CustomUserDetailsService;
import ra.demo.service.UserService;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final CustomUserDetailsService userDetailsService;

    @Value("${jwt-refresh-expire}")
    private long jwtRefreshExpiration;

    // Auth Register Customer/Driver ,Login Customer/Driver, send otp for Driver, refresh token, logout
    @Override
    @Transactional
    public User registerCustomer(CustomerRegisterRequest customerRegisterRequest) {
        Role role = getRoleOrThrow(RoleEnums.ROLE_CUSTOMER);

        return userRepository.findByPhoneNumber(customerRegisterRequest.getPhoneNumber())
                .map(existingUser -> {
                    // Kiểm tra xem user này đã có ROLE_CUSTOMER chưa
                    boolean hasCustomerRole = existingUser.getRoles().stream()
                            .anyMatch(r -> r.getRoleName().equals(RoleEnums.ROLE_CUSTOMER));

                    if (hasCustomerRole) {
                        throw new BadRequestException("Số điện thoại này đã được đăng ký!");
                    }

                    // Nếu là Driver đăng ký thêm Customer -> Thêm ROLE_CUSTOMER & Mật khẩu
                    existingUser.getRoles().add(role);
                    existingUser.setPassword(passwordEncoder.encode(customerRegisterRequest.getPassword()));

                    // Cập nhật bổ sung tên/email nếu tài khoản cũ chưa có
                    if (existingUser.getFullName() == null || existingUser.getFullName().isEmpty()) {
                        existingUser.setFullName(customerRegisterRequest.getFullName());
                    }
                    if (existingUser.getEmail() == null || existingUser.getEmail().isEmpty()) {
                        existingUser.setEmail(customerRegisterRequest.getEmail());
                    }

                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // Tạo mới hoàn toàn nếu chưa từng tồn tại SĐT này
                    User user = User.builder()
                            .phoneNumber(customerRegisterRequest.getPhoneNumber())
                            .password(passwordEncoder.encode(customerRegisterRequest.getPassword()))
                            .fullName(customerRegisterRequest.getFullName())
                            .email(customerRegisterRequest.getEmail())
                            .status(AccountStatus.ACTIVE)
                            .roles(new HashSet<>(Set.of(role)))
                            .build();
                    return userRepository.save(user);
                });
    }

    @Override
    @Transactional
    public User registerDriver(DriverRegisterRequest driverRegisterRequest) {
        Role role = getRoleOrThrow(RoleEnums.ROLE_DRIVER);

        return userRepository.findByPhoneNumber(driverRegisterRequest.getPhoneNumber())
                .map(existingUser -> {
                    // Kiểm tra xem user đã có ROLE_DRIVER chưa
                    boolean hasDriverRole = existingUser.getRoles().stream()
                            .anyMatch(r -> r.getRoleName().equals(RoleEnums.ROLE_DRIVER));

                    if (hasDriverRole) {
                        throw new BadRequestException("Số điện thoại này đã được đăng ký!");
                    }

                    // SĐT đã tồn tại (ví dụ đã làm Customer) -> Bổ sung thêm ROLE_DRIVER
                    existingUser.getRoles().add(role);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // Tạo mới Driver
                    User newDriver = User.builder()
                            .phoneNumber(driverRegisterRequest.getPhoneNumber())
                            .fullName(driverRegisterRequest.getFullName())
                            .email(driverRegisterRequest.getEmail())
                            .status(AccountStatus.ACTIVE)
                            .roles(new HashSet<>(Set.of(role)))
                            .build();
                    return userRepository.save(newDriver);
                });
    }

    @Override
    @Transactional
    public JwtResponse loginCustomer(CustomerLoginRequest customerLoginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(customerLoginRequest.getPhoneNumber(), customerLoginRequest.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtProvider.generateToken(userDetails);
        RefreshToken refreshToken = createAndSaveRefreshToken(userDetails.getUsername());

        return buildJwtResponse(userDetails, accessToken, refreshToken.getRefreshToken());
    }

    @Override
    @Transactional
    public JwtResponse loginDriver(DriverLoginRequest driverLoginRequest) {
        boolean isValidOtp = otpService.validateOtp(driverLoginRequest.getPhoneNumber(), driverLoginRequest.getOtp());

        if (!isValidOtp) {
            throw new BadRequestException("Mã OTP không chính xác hoặc đã hết hạn!");
        }

        User user = userRepository.findByPhoneNumber(driverLoginRequest.getPhoneNumber()).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy tài khoản Tài xế!")
        );

        if (!hasRole(user, RoleEnums.ROLE_DRIVER)) {
            throw new BadRequestException("Tài khoản này không có quyền Tài xế!");
        }

        if (user.getStatus() == AccountStatus.DRIVER_LOCKED) {
            throw new BadRequestException("Tài khoản Tài xế của bạn đã bị khóa. Vui lòng liên hệ Admin!");
        }
        if (user.getStatus() == AccountStatus.DRIVER_DELETED) {
            throw new BadRequestException("Tài khoản Tài xế của bạn đã bị xóa khỏi hệ thống!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(user.getPhoneNumber());
        String accessToken = jwtProvider.generateToken(userDetails);
        RefreshToken refreshToken = createAndSaveRefreshToken(userDetails.getUsername());

        return buildJwtResponse(userDetails, accessToken, refreshToken.getRefreshToken());
    }

    @Override
    public String sendOtpForDriver(SendOtpRequest sendOtpRequest) {
        User user = userRepository.findByPhoneNumber(sendOtpRequest.getPhoneNumber()).orElseThrow(() ->
                new ResourceNotFoundException("Số điện thoại tài xế chưa tồn tại trên hệ thống!")
        );

        if (!hasRole(user, RoleEnums.ROLE_DRIVER)) {
            throw new BadRequestException("Số điện thoại này không có quyền Tài xế!");
        }

        String otp = otpService.generateAndSaveOtp(sendOtpRequest.getPhoneNumber());
        log.info("Mã OTP gửi tới SĐT {}: {}", sendOtpRequest.getPhoneNumber(), otp);

        return "Mã OTP đã được gửi thành công. Vui lòng kiểm tra tin nhắn!";
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponse refreshToken(String refreshToken) {
        // 1. Tìm token trong Database
        RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken).orElseThrow(() ->
                new ResourceNotFoundException("Refresh Token không tồn tại trên hệ thống!")
        );

        // 2. Kiểm tra token đã bị thu hồi chưa
        if (Boolean.TRUE.equals(token.getInvoke())) {
            throw new BadRequestException("Refresh Token đã bị thu hồi/vô hiệu hóa!");
        }

        // 3. Kiểm tra thời hạn lưu trong DB
        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh Token đã hết hạn. Vui lòng đăng nhập lại!");
        }

        // 4. Load User Details & Tạo Access Token mới
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(token.getPhoneNumber());
        String newAccessToken = jwtProvider.generateToken(userDetails);

        return buildJwtResponse(userDetails, newAccessToken, refreshToken);
    }

    @Override
    @Transactional
    public void logout(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            throw new BadRequestException("Thông tin người dùng không hợp lệ!");
        }
        refreshTokenRepository.deleteByPhoneNumber(phoneNumber);
        log.info("Đăng xuất thành công và xóa Refresh Token của SĐT: {}", phoneNumber);
    }

    @Override
    public void adminChangeAccountStatus(Long adminUserId, Long targetUserId, AccountStatus accountStatus) {
        if (adminUserId.equals(targetUserId)) {
            throw new BadRequestException("Bạn không thể tự thay đổi hoặc vô hiệu hóa trạng thái tài khoản của chính mình!");
        }

        User user = userRepository.findById(targetUserId).orElseThrow(() -> new
                ResourceNotFoundException("Không tìm thấy tài khoản người dùng với ID: " + targetUserId)
        );

        user.setStatus(accountStatus);
        userRepository.save(user);
        revokeUserRefreshToken(user.getPhoneNumber());
    }

    @Override
    @Transactional
    public void customerChangeAccountStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("Không tìm thấy thông tin tài khoản Khách hàng!")
        );

        if (!hasRole(user, RoleEnums.ROLE_CUSTOMER)) {
            throw new BadRequestException("Tài khoản này không phải là Khách hàng!");
        }

        user.setStatus(AccountStatus.CUSTOMER_DELETED);
        userRepository.save(user);
        revokeUserRefreshToken(user.getPhoneNumber());
    }

    @Override
    @Transactional
    public void driverChangeAccountStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản Tài xế!"));

        if (!hasRole(user, RoleEnums.ROLE_DRIVER)) {
            throw new BadRequestException("Tài khoản này không phải là Tài xế!");
        }

        user.setStatus(AccountStatus.DRIVER_DELETED);
        userRepository.save(user);
        revokeUserRefreshToken(user.getPhoneNumber());
    }

    private Role getRoleOrThrow(RoleEnums roleEnum) {
        return roleRepository.findByRoleName(roleEnum)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa khởi tạo " + roleEnum + " trong hệ thống!"));
    }

    private boolean hasRole(User user, RoleEnums roleEnum) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals(roleEnum));
    }

    private RefreshToken createAndSaveRefreshToken(String phoneNumber) {
        refreshTokenRepository.deleteByPhoneNumber(phoneNumber);
        RefreshToken refreshToken = RefreshToken.builder()
                .phoneNumber(phoneNumber)
                .expiryDate(Instant.now().plusMillis(jwtRefreshExpiration))
                .refreshToken(UUID.randomUUID().toString())
                .invoke(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    private void revokeUserRefreshToken(String phoneNumber) {
        refreshTokenRepository.findByPhoneNumber(phoneNumber)
                .ifPresent(token -> {
                    token.setInvoke(true);
                    refreshTokenRepository.save(token);
                });
    }

    private JwtResponse buildJwtResponse(CustomUserDetails userDetails, String accessToken, String refreshToken) {
        return JwtResponse.builder()
                .phoneNumber(userDetails.getPhoneNumber())
                .fullName(userDetails.getFullName())
                .email(userDetails.getEmail())
                .status(userDetails.getStatus().toString())
                .authorities(userDetails.getAuthorities())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
