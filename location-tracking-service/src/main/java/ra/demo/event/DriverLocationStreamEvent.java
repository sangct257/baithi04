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
    private Long driverId;
    private Long tripId;
    private Double lat;
    private Double lng;
    private String vehicleType;
}