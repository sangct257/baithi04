package ra.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationStreamEvent {
    private Long driverId;      // ID của Tài xế
    private Long tripId;        // ID Chuyến đi (null nếu tài xế đang rảnh, có giá trị nếu đang chở khách)
    private Double lat;         // Vĩ độ (Latitude)
    private Double lng;         // Kinh độ (Longitude)
    private String vehicleType; // Loại xe (BIKE, CAR, ...)
}