package ra.demo.service;

import ra.demo.dto.request.DriverLocationRequest;
import ra.demo.dto.response.NearbyDriverResponse;

import java.util.List;

public interface LocationService {
    void updateDriverLocation(DriverLocationRequest request);
    List<NearbyDriverResponse> findNearbyDrivers(Double lat, Double lng, Double radiusKm, String vehicleType);
    void removeDriverLocation(Long driverId);
    String getH3Index(Double lat, Double lng); // Hàm tiện ích Uber H3
}
