package ra.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constains.PaymentMethod;
import ra.demo.constains.VehicleType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestEvent {
    private Long tripId;
    private Long customerId;
    private String pickupAddress; // Địa chỉ điểm đón
    private Double pickupLat; // Vĩ độ điểm đón
    private Double pickupLng; // Kinh độ điểm đón
    private String dropoffAddress; // Địa chỉ điểm trả
    private Double dropoffLat; // Vĩ độ điểm trả
    private Double dropoffLng; // Kinh độ điểm trả
    private BigDecimal amount;
    private VehicleType vehicleType;
    private PaymentMethod paymentMethod;
}