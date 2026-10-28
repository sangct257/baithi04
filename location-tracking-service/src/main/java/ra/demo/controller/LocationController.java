package ra.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ra.demo.dto.request.DriverLocationRequest;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.NearbyDriverResponse;
import ra.demo.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // 1. Tài xế đẩy GPS định kỳ (3-5 giây/lần)
    @PostMapping("/driver")
    public ResponseEntity<ApiResponse<Void>> updateLocation(@Valid @RequestBody DriverLocationRequest request) {
        locationService.updateDriverLocation(request);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cập nhật vị trí GPS thành công",
                null,
                null,
                HttpStatus.OK
        ));
    }

    // 2. Tìm tài xế xung quanh điểm đón
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyDriverResponse>>> findNearbyDrivers(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "3.0") Double radiusKm,
            @RequestParam(required = false) String vehicleType) {

        List<NearbyDriverResponse> drivers = locationService.findNearbyDrivers(lat, lng, radiusKm, vehicleType);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lấy danh sách tài xế xung quanh thành công",
                drivers,
                null,
                HttpStatus.OK
        ));
    }

    // 3. Xóa tài xế khỏi danh sách rảnh khi Off/Nhận chuyến
    @DeleteMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<Void>> removeDriver(@PathVariable Long driverId) {
        locationService.removeDriverLocation(driverId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Đã xóa vị trí tài xế thành công",
                null,
                null,
                HttpStatus.OK
        ));
    }


}