package ra.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.VehicleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationRequest {

    private Long driverId;

    @NotNull(message = "Vĩ độ không được để trống")
    private Double latitude;

    @NotNull(message = "Kinh độ không được để trống")
    private Double longitude;

    private VehicleType vehicleType;
}