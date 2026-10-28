package ra.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.VehicleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePricingRuleRequest {
    private VehicleType vehicleType; // BIKE, CAR_4_SEAT, CAR_7_SEAT
    private Double baseFare;        // Giá mở cửa 2km đầu (VNĐ)
    private Double perKmRate;       // Giá mỗi km tiếp theo (VNĐ)
}
