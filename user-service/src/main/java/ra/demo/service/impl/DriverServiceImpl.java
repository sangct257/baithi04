package ra.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.constants.DriverStatus;
import ra.demo.dto.request.DriverProfileRegisterRequest;
import ra.demo.dto.response.DriverProfileResponse;
import ra.demo.entity.DriverProfile;
import ra.demo.entity.User;
import ra.demo.entity.Vehicle;
import ra.demo.event.DriverStatusEvent;
import ra.demo.exception.BadRequestException;
import ra.demo.exception.ConflictException;
import ra.demo.exception.ResourceNotFoundException;
import ra.demo.repository.DriverProfileRepository;
import ra.demo.repository.UserRepository;
import ra.demo.repository.VehicleRepository;
import ra.demo.service.DriverService;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImpl implements DriverService {
    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public DriverProfileResponse createOrUpdateProfile(Long userId, DriverProfileRegisterRequest driverProfileRegisterRequest) {
        // 1. Kiểm tra tính duy nhất của Bằng lái, CCCD, Biển số xe (loại trừ chính userId hiện tại)
        validateUniqueDriverData(userId, driverProfileRegisterRequest);

        // 2. Tìm Profile đã tồn tại hoặc tạo mới Profile gắn với User
        DriverProfile profile = driverProfileRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

                    return DriverProfile.builder()
                            .user(user)
                            .status(DriverStatus.OFFLINE)
                            .rating(5.0)
                            .isApproved(false)
                            .build();
                });

        // 3. Cập nhật thông tin Hồ sơ
        profile.setDriverLicenseNumber(driverProfileRegisterRequest.getDriverLicenseNumber());
        profile.setIdentityCard(driverProfileRegisterRequest.getIdentityCard());

        // 4. Khởi tạo/Cập nhật thông tin Phương tiện (Vehicle)
        Vehicle vehicle = profile.getVehicle();
        if (vehicle == null) {
            vehicle = new Vehicle();
            vehicle.setDriverProfile(profile); // Gán quan hệ 2 chiều
            profile.setVehicle(vehicle);
        }

        vehicle.setLicensePlate(driverProfileRegisterRequest.getLicensePlate());
        vehicle.setBrand(driverProfileRegisterRequest.getBrand());
        vehicle.setModel(driverProfileRegisterRequest.getModel());
        vehicle.setVehicleType(driverProfileRegisterRequest.getVehicleType());

        // 5. Lưu Profile xuống DB (CascadeType.ALL sẽ tự động lưu cả Vehicle)
        DriverProfile savedProfile = driverProfileRepository.save(profile);

        // 6. Chuyển đổi sang Response DTO
        return mapToResponse(savedProfile);
    }

    /**
     * Lấy chi tiết hồ sơ tài xế theo userId
     */
    @Override
    @Transactional(readOnly = true) // Tối ưu hiệu năng truy vấn chỉ đọc
    public DriverProfileResponse getProfile(Long userId) {
        DriverProfile profile = driverProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Chưa tìm thấy hồ sơ tài xế. Vui lòng cập nhật thông tin hồ sơ!"));

        return mapToResponse(profile);
    }

    /**
     * Bật / Tắt trạng thái hoạt động của tài xế (ONLINE <-> OFFLINE)
     */
    @Override
    @Transactional
    public DriverProfileResponse changeStatus(Long userId) {
        DriverProfile profile = driverProfileRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("Chưa tìm thấy hồ sơ tài xế!")
        );

        // 1. Ràng buộc: Hồ sơ phải được Admin phê duyệt mới được đổi trạng thái hoạt động
        if (!Boolean.TRUE.equals(profile.getIsApproved())) {
            throw new BadRequestException("Hồ sơ tài xế của bạn chưa được Admin phê duyệt. Không thể bật hoạt động!");
        }

        // 2. Nếu muốn bật ONLINE nhưng đang bị khóa 30s -> Chặn và ném Exception
        if (profile.getStatus() == DriverStatus.OFFLINE) {
            String penaltyKey = "driver:penalty:" + userId;
            Boolean isPenalized = redisTemplate.hasKey(penaltyKey);

            if (Boolean.TRUE.equals(isPenalized)) {
                Long remainSeconds = redisTemplate.getExpire(penaltyKey, TimeUnit.SECONDS);
                long seconds = remainSeconds != null && remainSeconds > 0 ? remainSeconds : 30;
                throw new BadRequestException("Bạn vừa bỏ lỡ chuyến đi! Hệ thống tạm khóa trạng thái hoạt động trong "
                        + seconds + " giây nữa.");
            }
        }

        // 3. Đảo trạng thái: ONLINE -> OFFLINE hoặc ngược lại
        DriverStatus newStatus = (profile.getStatus() == DriverStatus.ONLINE)
                ? DriverStatus.OFFLINE
                : DriverStatus.ONLINE;

        profile.setStatus(newStatus);
        DriverProfile savedProfile = driverProfileRepository.save(profile);

        DriverStatusEvent syncRequest = DriverStatusEvent.builder()
                .driverId(userId)
                .status(newStatus) // ONLINE hoặc OFFLINE
                .vehicleType(profile.getVehicle().getVehicleType())
                .build();

        // Bắn tin nhắn lên Kafka Topic tên là "driver-status-topic"
        kafkaTemplate.send("driver-status-topic", String.valueOf(userId), syncRequest);
        log.info("[USER SERVICE] Đã bắn Event đổi trạng thái tài xế #{}: {}", userId, newStatus);
        return mapToResponse(savedProfile);
    }

    /**
     * Admin thực hiện Duyệt / Hủy duyệt hồ sơ tài xế
     */
    @Override
    @Transactional
    public DriverProfileResponse approveDriverProfile(Long userId) {
        DriverProfile profile = driverProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ tài xế với ID: " + userId));

        // Đảo trạng thái phê duyệt (true <-> false)
        boolean newApprovedState = !Boolean.TRUE.equals(profile.getIsApproved());
        profile.setIsApproved(newApprovedState);

        // Nếu bị Admin hủy duyệt (false) -> Bắt buộc ép trạng thái hoạt động về OFFLINE ngay
        if (!newApprovedState) {
            profile.setStatus(DriverStatus.OFFLINE);
        }

        DriverProfile savedProfile = driverProfileRepository.save(profile);
        return mapToResponse(savedProfile);
    }

    private void validateUniqueDriverData(Long userId, DriverProfileRegisterRequest request) {
        if (driverProfileRepository.existsByDriverLicenseNumberAndIdNot(request.getDriverLicenseNumber(), userId)) {
            throw new ConflictException("Số giấy phép lái xe đã được sử dụng bởi tài khoản khác!");
        }

        if (driverProfileRepository.existsByIdentityCardAndIdNot(request.getIdentityCard(), userId)) {
            throw new ConflictException("Số CCCD/CMND đã được sử dụng bởi tài khoản khác!");
        }

        if (vehicleRepository.existsByLicensePlateAndDriverProfileIdNot(request.getLicensePlate(), userId)) {
            throw new ConflictException("Biển số xe đã được đăng ký trên hệ thống!");
        }
    }

    private DriverProfileResponse mapToResponse(DriverProfile profile) {
        Vehicle vehicle = profile.getVehicle();
        User user = profile.getUser();

        return DriverProfileResponse.builder()
                .userId(profile.getId()) // profile.getId() trùng với user.getId() nhờ @MapsId
                .fullName(user != null ? user.getFullName() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .driverLicenseNumber(profile.getDriverLicenseNumber())
                .identityCard(profile.getIdentityCard())
                .status(profile.getStatus())
                .rating(profile.getRating())
                .isApproved(profile.getIsApproved())
                .licensePlate(vehicle != null ? vehicle.getLicensePlate() : null)
                .brand(vehicle != null ? vehicle.getBrand() : null)
                .model(vehicle != null ? vehicle.getModel() : null)
                .vehicleType(vehicle != null ? vehicle.getVehicleType() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
