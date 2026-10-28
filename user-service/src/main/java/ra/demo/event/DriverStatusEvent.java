package ra.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.DriverStatus;
import ra.demo.constants.VehicleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatusEvent {
    private Long driverId;
    private DriverStatus status;
    private VehicleType vehicleType;
}