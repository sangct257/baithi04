package ra.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.VehicleType;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DriverProfileRegisterRequest {
    @NotBlank(message = "Số bằng lái không được để trống")
    private String driverLicenseNumber;

    @NotBlank(message = "Số CCCD/CMND không được để trống")
    private String identityCard;

    @NotBlank(message = "Biển số xe không được để trống")
    private String licensePlate;

    @NotBlank(message = "Hãng xe không được để trống")
    private String brand;

    @NotBlank(message = "Dòng xe không được để trống")
    private String model;

    @NotNull(message = "Loại phương tiện không được để trống")
    private VehicleType vehicleType;
}
