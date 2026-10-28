package ra.demo.initData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
import ra.demo.constants.AccountStatus;
import ra.demo.constants.DriverStatus;
import ra.demo.constants.RoleEnums;
import ra.demo.constants.VehicleType;
import ra.demo.entity.DriverProfile;
import ra.demo.entity.Role;
import ra.demo.entity.User;
import ra.demo.entity.Vehicle;
import ra.demo.repository.DriverProfileRepository;
import ra.demo.repository.RoleRepository;
import ra.demo.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


@Component
@RequiredArgsConstructor
@Slf4j
public class initDataAccount implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DriverProfileRepository driverProfileRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (roleRepository.findAll().isEmpty()) {
            List<Role> roles = List.of(
                    new Role(null, RoleEnums.ROLE_ADMIN, LocalDateTime.now(), LocalDateTime.now()),
                    new Role(null, RoleEnums.ROLE_CUSTOMER, LocalDateTime.now(), LocalDateTime.now()),
                    new Role(null, RoleEnums.ROLE_DRIVER, LocalDateTime.now(), LocalDateTime.now())
            );

            roleRepository.saveAll(roles);
            System.out.println("--------> Đã khởi tạo các role ban đầu thành công!");
        }

        Role adminRole = roleRepository.findByRoleName(RoleEnums.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_ADMIN!"));
        Role customerRole = roleRepository.findByRoleName(RoleEnums.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_CUSTOMER!"));
        Role driverRole = roleRepository.findByRoleName(RoleEnums.ROLE_DRIVER)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_DRIVER!"));

        // 2. Tạo tài khoản ADMIN mặc định để test
        String adminPhone = "0999999999";
        if (!userRepository.existsByPhoneNumber(adminPhone)) {

            User adminUser = User.builder()
                    .phoneNumber(adminPhone)
                    .password(passwordEncoder.encode("1234")) // Mã hóa mật khẩu 1234
                    .fullName("System Admin")
                    .email("admin@demo.com")
                    .status(AccountStatus.ACTIVE)
                    .roles(Set.of(adminRole))
                    .build();

            userRepository.save(adminUser);
            log.info("--------> Khởi tạo Admin (SĐT: 0999999999 / Pass: 1234) thành công!");
        }

        // 3. Khởi tạo tài khoản CUSTOMER & Tạo ví có sẵn 1.000.000 VNĐ
        String customerPhone = "0888888888";
        if (!userRepository.existsByPhoneNumber(customerPhone)) {
            User customerUser = User.builder()
                    .phoneNumber(customerPhone)
                    .password(passwordEncoder.encode("1234"))
                    .fullName("Nguyễn Văn Khách")
                    .email("customer@demo.com")
                    .status(AccountStatus.ACTIVE)
                    .roles(Set.of(customerRole))
                    .build();

            userRepository.save(customerUser);
            log.info("--------> Khởi tạo Customer (SĐT: 0888888888 / Pass: 1234) thành công!");
        }

        // 4. Khởi tạo tài khoản DRIVER & Tạo ví có sẵn 500.000 VNĐ
        String driverPhone = "0777777777";
        if (!userRepository.existsByPhoneNumber(driverPhone)) {
            User driverUser = User.builder()
                    .phoneNumber(driverPhone)
                    .password(passwordEncoder.encode("1234"))
                    .fullName("Trần Văn Tài Xế")
                    .email("driver@demo.com")
                    .status(AccountStatus.ACTIVE)
                    .roles(Set.of(driverRole))
                    .build();

            User savedDriver = userRepository.save(driverUser);

            // Tạo Hồ sơ Tài xế mặc định cho Tài xế vừa khởi tạo
            DriverProfile driverProfile = DriverProfile.builder()
                    .user(savedDriver)
                    .driverLicenseNumber("GPLX-0123456789")  // Bằng lái xe
                    .identityCard("001098765432")         // Số CCCD/CMND
                    .status(DriverStatus.OFFLINE)
                    .rating(5.0)
                    .isApproved(true) // Đã được ADMIN xác nhận
                    .build();


            Vehicle vehicle = Vehicle.builder()
                    .driverProfile(driverProfile)
                    .licensePlate("29S1-888.88") // Biển số xe
                    .brand("Honda") // Hãng xe
                    .model("Wave Alpha") // Dòng xe
                    .vehicleType(VehicleType.BIKE)
                    .build();

            driverProfile.setVehicle(vehicle);

            driverProfileRepository.save(driverProfile);

            log.info("--------> Khởi tạo Driver (SĐT: 0777777777 / Pass: 1234) thành công!");
        }
    }
}