package ra.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestEvent {
    private Long tripId;
    private Long customerId;
    // Bổ sung danh sách ID tài xế được nhận đề nghị chuyến
    private List<String> candidateDriverIds;
    private String pickupAddress; // Địa chỉ điểm đón
    private Double pickupLat; // Vĩ độ điểm đón
    private Double pickupLng; // Kinh độ điểm đón
    private String dropoffAddress; // Địa chỉ điểm trả
    private Double dropoffLat; // Vĩ độ điểm trả
    private Double dropoffLng; // Kinh độ điểm trả
    private BigDecimal amount;
    private String vehicleType;
    private String paymentMethod;
}