package ra.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ra.demo.dto.request.CreateTripRequest;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.TripResponse;
import ra.demo.service.TripService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {
    private final TripService tripService;

    // Chức năng khách hàng
    /**
     * Khách hàng tạo chuyến đi mới
     * POST /api/v1/trips
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody CreateTripRequest request) {
        TripResponse response = tripService.createTrip(request);
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Đặt chuyến đi thành công!",
                response,
                null,
                HttpStatus.CREATED
        ),HttpStatus.CREATED);
    }


    /**
     * Khách hàng xem chi tiết chuyến đi hiện tại
     * GET /api/v1/trips/{tripId}
     */
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getCustomerMyTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.getCustomerMyTrip(tripId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lấy thông tin chuyến đi thành công",
                response,
                null,
                HttpStatus.OK
        ));
    }

    /**
     * Khách hàng xem lịch sử tất cả chuyến đi
     * GET /api/v1/trips/customer/history
     */
    @GetMapping("/customer/history")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getCustomerTripHistory() {
        List<TripResponse> history = tripService.getCustomerTripHistory();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lấy lịch sử chuyến đi thành công",
                history,
                null,
                HttpStatus.OK
        ));
    }

    /**
     * Khách hàng hủy chuyến đi
     * PUT /api/v1/trips/{tripId}/customer-cancel?reason=...
     */
    @PutMapping("/{tripId}/customer-cancel")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTripByCustomer(
            @PathVariable Long tripId,
            @RequestParam(required = false) String reason) {
        TripResponse response = tripService.cancelTripByCustomer(tripId, reason);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Hủy chuyến đi thành công",
                response,
                null,
                HttpStatus.OK
        ));
    }

    // Chức năng tài xế
    /**
     * Tài xế nhận chuyến đi
     * PUT /api/v1/trips/{tripId}/accept
     */
    @PutMapping("/{tripId}/accept")
    public ResponseEntity<ApiResponse<TripResponse>> acceptTrip(@PathVariable Long tripId) {
        TripResponse response = tripService.acceptTrip(tripId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Nhận chuyến thành công! Vui lòng di chuyển đến điểm đón",
                response,
                null,
                HttpStatus.OK
        ));
    }

    /**
     * Tài xế cập nhật trạng thái tiến trình (ARRIVED -> IN_PROGRESS -> COMPLETED)
     * PUT /api/v1/trips/{tripId}/status
     */
    @PutMapping("/{tripId}/status")
    public ResponseEntity<ApiResponse<TripResponse>> updateTripStatusByDriver(@PathVariable Long tripId) {
        TripResponse response = tripService.updateTripStatusByDriver(tripId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cập nhật trạng thái chuyến đi thành công",
                response,
                null,
                HttpStatus.OK
        ));
    }

    /**
     * Tài xế hủy chuyến
     * PUT /api/v1/trips/{tripId}/driver-cancel?reason=...
     */
    @PutMapping("/{tripId}/driver-cancel")
    public ResponseEntity<ApiResponse<TripResponse>> cancelTripByDriver(
            @PathVariable Long tripId,
            @RequestParam(required = false) String reason) {
        TripResponse response = tripService.cancelTripByDriver(tripId, reason);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Đã hủy chuyến đi",
                response,
                null,
                HttpStatus.OK
        ));
    }

    /**
     * Tài xế xem lịch sử chuyến đi đã chạy
     * GET /api/v1/trips/driver/history
     */
    @GetMapping("/driver/history")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getDriverTripHistory() {
        List<TripResponse> history = tripService.getDriverTripHistory();
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lấy lịch sử nhận chuyến thành công",
                history,
                null,
                HttpStatus.OK
        ));
    }
}
