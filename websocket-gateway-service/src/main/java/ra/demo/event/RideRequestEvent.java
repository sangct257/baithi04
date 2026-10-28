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
    private List<String> candidateDriverIds;
    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;
    private String dropoffAddress;
    private Double dropoffLat;
    private Double dropoffLng;
    private BigDecimal amount;
    private String vehicleType;
    private String paymentMethod;
}