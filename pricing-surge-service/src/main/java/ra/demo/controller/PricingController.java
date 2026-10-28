package ra.demo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ra.demo.constants.VehicleType;
import ra.demo.dto.request.CalculatePriceRequest;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.PriceCalculationResponse;
import ra.demo.service.PricingService;

import lombok.extern.slf4j.Slf4j;
import ra.demo.dto.request.UpdatePricingRuleRequest;
import ra.demo.exception.AccessDeniedException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
@Slf4j
public class PricingController {
    private final PricingService pricingService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 1. API Chơi thử tính cước chuyến đi
     * POST /api/v1/pricing/calculate
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<PriceCalculationResponse>> calculateFare(@RequestBody CalculatePriceRequest request) {
        PriceCalculationResponse response = pricingService.calculateFare(request);
        return new ResponseEntity<>(new ApiResponse<>(
                true,
                "Tính cước chuyến đi thành công",
                response,
                null,
                HttpStatus.OK
        ), HttpStatus.OK);
    }

    /**
     * 2. API Cập nhật bảng giá cho NHIỀU LOẠI XE CÙNG LÚC (DÀNH CHO ADMIN)
     * PUT /api/v1/pricing/rules
     */
    @PutMapping("/rules")
    public ResponseEntity<ApiResponse<String>> updatePricingRule(@RequestBody List<UpdatePricingRuleRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Danh sách cập nhật giá không được để trống!");
        }

        StringBuilder resultMessage = new StringBuilder("Đã cập nhật các loại xe: ");

        for (UpdatePricingRuleRequest req : requests) {
            if (req.getVehicleType() == null) continue;

            String key = "pricing:rules:" + req.getVehicleType().name();

            if (req.getBaseFare() != null && req.getBaseFare() > 0) {
                redisTemplate.opsForHash().put(key, "baseFare", String.valueOf(Math.round(req.getBaseFare())));
            }
            if (req.getPerKmRate() != null && req.getPerKmRate() > 0) {
                redisTemplate.opsForHash().put(key, "perKmRate", String.valueOf(Math.round(req.getPerKmRate())));
            }

            resultMessage.append(req.getVehicleType().name()).append(" ");
        }

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Cập nhật danh sách bảng giá thành công!",
                resultMessage.toString().trim(),
                null,
                HttpStatus.OK
        ));
    }

    /**
     * 3. API Nạp toàn bộ Bảng giá mẫu mặc định vào REDIS (Dành cho Admin Seeding)
     * POST /api/v1/pricing/seed-rules
     */
    @PostMapping("/seed-rules")
    public ResponseEntity<ApiResponse<String>> seedPricingRulesToRedis() {
        redisTemplate.opsForHash().put("pricing:rules:BIKE", "baseFare", "14000");
        redisTemplate.opsForHash().put("pricing:rules:BIKE", "perKmRate", "5000");

        redisTemplate.opsForHash().put("pricing:rules:CAR_4_SEAT", "baseFare", "18000");
        redisTemplate.opsForHash().put("pricing:rules:CAR_4_SEAT", "perKmRate", "12000");

        redisTemplate.opsForHash().put("pricing:rules:CAR_7_SEAT", "baseFare", "22000");
        redisTemplate.opsForHash().put("pricing:rules:CAR_7_SEAT", "perKmRate", "16000");

        log.info("[ADMIN SEED PRICING] Nạp bảng giá mặc định thành công vào Redis!");

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Đã nạp quy tắc giá mẫu vào Redis Hash thành công!",
                null,
                null,
                HttpStatus.OK
        ));
    }

    /**
     * 4. API Giả lập Tăng giá SURGE ZONE
     * POST /api/v1/pricing/mock-surge?lat=21.0378&lng=105.7839&rate=1.5
     */
    @PostMapping("/mock-surge")
    public ResponseEntity<ApiResponse<String>> mockSurgeZone(@RequestParam Double lat,
                                                             @RequestParam Double lng,
                                                             @RequestParam String rate) {
        double truncatedLat = Math.floor(lat * 100.0) / 100.0;
        double truncatedLng = Math.floor(lng * 100.0) / 100.0;
        String zoneKey = String.format(java.util.Locale.US, "%.2f_%.2f", truncatedLat, truncatedLng);

        redisTemplate.opsForValue().set("surge:zone:" + zoneKey, rate);

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Đã bật Surge Pricing " + rate + "x cho Zone " + zoneKey,
                null,
                null,
                HttpStatus.OK
        ));
    }

    /**
     * 5. API Xem toàn bộ giá trị đang lưu trong Redis
     * GET /api/v1/pricing/check-redis
     */
    @GetMapping("/check-redis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkRedisData() {
        Map<String, Object> data = new HashMap<>();

        for (VehicleType type : VehicleType.values()) {
            Map<Object, Object> rule = redisTemplate.opsForHash().entries("pricing:rules:" + type.name());
            data.put("Rule_" + type.name(), rule);
        }

        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Lấy dữ liệu Redis thành công",
                data,
                null,
                HttpStatus.OK
        ));
    }
}
