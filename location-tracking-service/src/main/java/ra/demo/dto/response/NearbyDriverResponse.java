package ra.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.VehicleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyDriverResponse {
    private Long driverId;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private VehicleType vehicleType;
}