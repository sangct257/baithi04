package ra.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.NearbyDriverResponse;

import java.util.List;

@FeignClient(name = "location-tracking-service", path = "${application.config.location-service-url:http://localhost:8085}")
public interface LocationClient {

    @GetMapping("/api/v1/location/nearby")
    ApiResponse<List<NearbyDriverResponse>> findNearbyDrivers(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radiusKm") Double radiusKm,
            @RequestParam("vehicleType") String vehicleType
    );
}
