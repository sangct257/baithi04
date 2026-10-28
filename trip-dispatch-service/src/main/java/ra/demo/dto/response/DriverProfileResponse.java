package ra.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.DriverStatus;
import ra.demo.constants.VehicleType;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DriverProfileResponse {
    private Long userId;
    private String fullName;
    private String phoneNumber;
    private String driverLicenseNumber; // Số bằng lái
    private String identityCard; // Số CCCD/CMND
    private DriverStatus status;
    private Double rating; // đánh giá
    private Boolean isApproved; // trạng thái duyệt hồ sơ

    // Thông tin phương tiện
    private String licensePlate; // Biển số xe
    private String brand; // Hãng xe (Honda, Toyota...)
    private String model; // Dòng xe (Wave Alpha, Vios...)
    private VehicleType vehicleType; // BIKE, CAR_4_SEAT...

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
