package ra.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.VehicleType;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class CalculatePriceRequest {
    private String pickupAddress; // Địa chỉ điểm đón
    private Double pickupLat; // Vĩ độ điểm đón
    private Double pickupLng; // Kinh độ điểm đón
    private String dropoffAddress; // Địa chỉ điểm trả
    private Double dropoffLat; // Vĩ độ điểm trả
    private Double dropoffLng; // Kinh độ điểm trả
    private VehicleType vehicleType;
}