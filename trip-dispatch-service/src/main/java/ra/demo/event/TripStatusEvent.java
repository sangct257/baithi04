package ra.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.PaymentMethod;
import ra.demo.constants.TripStatus;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStatusEvent {
    private Long tripId;
    private Long customerId;
    private Long driverId;
    private BigDecimal fareAmount;
    private PaymentMethod paymentMethod;
    private TripStatus tripStatus; // ACCEPTED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED
    private String cancelReason;
    private String cancelledBy;
}
