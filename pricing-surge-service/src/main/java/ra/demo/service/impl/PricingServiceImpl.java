package ra.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ra.demo.constants.VehicleType;
import ra.demo.dto.request.CalculatePriceRequest;
import ra.demo.dto.response.GeocodingResponse;
import ra.demo.dto.response.PriceCalculationResponse;
import ra.demo.exception.ResourceNotFoundException;
import ra.demo.service.PricingService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {
    // Inject StringRedisTemplate để thao tác với Redis Cache (String, Hash, Geo...)
    private final StringRedisTemplate redisTemplate;
    // Inject RestTemplate để thực hiện gọi API ra bên ngoài (LocationIQ, OSRM)
    private final RestTemplate restTemplate;

    // Lấy URL của OSRM Server từ file application.yml (Default: OSRM public)
    @Value("${osrm.url}")
    private String osrmUrl;

    // Lấy API Key LocationIQ từ application.yml dùng cho Geocoding
    @Value("${locationiq.api.key}")
    private String locationIqApiKey;

    // TÍNH GIÁ CƯỚC CHUYẾN ĐI
    @Override
    public PriceCalculationResponse calculateFare(CalculatePriceRequest calculatePriceRequest) {
        // 1. TÁC VỤ 1: Định vị điểm ĐÓN (Pickup) - Chạy bất đồng bộ
        CompletableFuture<GeocodingResponse> pickupFuture = CompletableFuture.supplyAsync(() -> {
            // Ưu tiên 1: Nếu client gửi Địa chỉ dạng chữ -> Đổi ra Tọa độ (Geocoding)
            if (calculatePriceRequest.getPickupAddress() != null && !calculatePriceRequest.getPickupAddress().isBlank()) {
                return getCoordinatesFromAddress(calculatePriceRequest.getPickupAddress());
            }
            // Ưu tiên 2: Nếu client đã truyền sẵn Tọa độ Lat/Lng -> Dùng trực tiếp
            if (calculatePriceRequest.getPickupLat() != null && calculatePriceRequest.getPickupLng() != null) {
                return new GeocodingResponse(calculatePriceRequest.getPickupLat(), calculatePriceRequest.getPickupLng());
            }
            return null;
        });

        // 2. TÁC VỤ 2: Định vị điểm TRẢ (Dropoff) - Chạy bất đồng bộ song song với Tác vụ 1
        CompletableFuture<GeocodingResponse> dropoffFuture = CompletableFuture.supplyAsync(() -> {
            // Ưu tiên 1: Nếu client gửi Địa chỉ dạng chữ -> Đổi ra Tọa độ (Geocoding)
            if (calculatePriceRequest.getDropoffAddress() != null && !calculatePriceRequest.getDropoffAddress().isBlank()) {
                return getCoordinatesFromAddress(calculatePriceRequest.getDropoffAddress());
            }
            // Ưu tiên 2: Nếu client đã truyền sẵn Tọa độ Lat/Lng -> Dùng trực tiếp
            if (calculatePriceRequest.getDropoffLat() != null && calculatePriceRequest.getDropoffLng() != null) {
                return new GeocodingResponse(calculatePriceRequest.getDropoffLat(), calculatePriceRequest.getDropoffLng());
            }
            return null;
        });

        // 3. ĐỒNG BỘ HÓA: Chờ cả 2 tác vụ Geocoding đón/trả hoàn thành song song để tiết kiệm thời gian
        CompletableFuture.allOf(pickupFuture, dropoffFuture).join();

        // Lấy kết quả từ 2 Future sau khi đã chạy xong
        GeocodingResponse pickupGeo = pickupFuture.join();
        GeocodingResponse dropoffGeo = dropoffFuture.join();

        // Bắt lỗi nếu không tìm được tọa độ điểm đón hoặc điểm trả
        if (pickupGeo == null || dropoffGeo == null ||
                pickupGeo.getLat() == null || pickupGeo.getLng() == null ||
                dropoffGeo.getLat() == null || dropoffGeo.getLng() == null) {
            throw new ResourceNotFoundException("Vui lòng nhập đầy đủ địa chỉ hoặc tọa độ điểm đón/trả!");
        }

        Double pickupLat = pickupGeo.getLat();  // Vĩ độ điểm đón
        Double pickupLng = pickupGeo.getLng();  // Kinh độ điểm đón
        Double dropoffLat = dropoffGeo.getLat(); // Vĩ độ điểm trả
        Double dropoffLng = dropoffGeo.getLng(); // Kinh độ điểm trả

        // 4. BẢN ĐỒ & TIẾN TRÌNH: Gọi OSRM API lấy Khoảng cách (km) & Thời gian thực tế (phút)
        Map<String, Object> mapData = getRouteFromOsrm(pickupLat, pickupLng, dropoffLat, dropoffLng);
        Double distanceKm = (Double) mapData.get("distanceKm");
        Integer durationMinutes = (Integer) mapData.get("durationMinutes");

        // 5. DYNAMIC PRICING: Tính hệ số tăng giá Surge Multiplier dựa trên mật độ nhu cầu/tài xế tại khu vực đón
        Double surgeMultiplier = calculateDynamicSurge(pickupLat, pickupLng, calculatePriceRequest.getVehicleType());

        // 6. TÍNH TIỀN: Áp dụng công thức và giá cấu hình từ Redis/Mặc định để ra số tiền cuối cùng
        BigDecimal fareAmount = computeFare(distanceKm, calculatePriceRequest.getVehicleType(), surgeMultiplier);

        // Build Response trả về kết quả tính toán chi tiết
        return PriceCalculationResponse.builder()
                .distanceKm(distanceKm)
                .durationMinutes(durationMinutes)
                .fareAmount(fareAmount)
                .surgeMultiplier(surgeMultiplier)
                .pickupLat(pickupLat)
                .pickupLng(pickupLng)
                .dropoffLat(dropoffLat)
                .dropoffLng(dropoffLng)
                .build();
    }

    // HÀM CHUYỂN ĐỔI ĐỊA CHỈ THÀNH TỌA ĐỘ (Geocoding)
    // Cơ chế: Đọc Cache Redis -> Nếu không có thì Gọi LocationIQ API -> Lưu Cache 7 ngày
    private GeocodingResponse getCoordinatesFromAddress(String address) {
        String normalized = normalizeAddress(address);
        String cacheKey = "geo:" + normalized;

        // BƯỚC 1: Đọc từ Redis Cache để giảm thời gian phản hồi và tiết kiệm quota API
        try {
            String cachedGeo = redisTemplate.opsForValue().get(cacheKey);
            if (cachedGeo != null) {
                String[] parts = cachedGeo.split(",");
                log.info("[REDIS GEO CACHE HIT] '{}' -> Lat: {}, Lng: {}", address, parts[0], parts[1]);
                return createGeocodingResponse(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
            }
        } catch (Exception e) {
            log.warn("[REDIS CACHE READ WARN] Không thể đọc cache geocoding: {}", e.getMessage());
        }

        // Tối ưu chuỗi query cho chính xác ở thị trường Việt Nam
        String queryAddress = address.toLowerCase().contains("việt nam") ? address : address + ", Việt Nam";

        // BƯỚC 2: Gọi Third-Party Geocoding API (LocationIQ)
        if (locationIqApiKey != null && !locationIqApiKey.isBlank()) {
            try {
                String url = UriComponentsBuilder.fromUriString("https://us1.locationiq.com/v1/search")
                        .queryParam("key", locationIqApiKey)
                        .queryParam("q", queryAddress)
                        .queryParam("format", "json")
                        .queryParam("limit", "1")
                        .build().toUriString();

                ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
                if (response.getBody() != null && !response.getBody().isEmpty()) {
                    Map<String, Object> firstResult = (Map<String, Object>) response.getBody().get(0);
                    Double lat = Double.parseDouble(firstResult.get("lat").toString());
                    Double lng = Double.parseDouble(firstResult.get("lon").toString());

                    log.info("[LOCATIONIQ SUCCESS] '{}' -> Lat: {}, Lng: {}", queryAddress, lat, lng);
                    // BƯỚC 3: Lưu tọa độ vừa lấy được vào Redis Cache với TTL là 7 ngày
                    saveGeoCache(cacheKey, lat, lng);
                    return createGeocodingResponse(lat, lng);
                }
            } catch (Exception e) {
                log.error("[LOCATIONIQ ERROR] Lỗi gọi LocationIQ: {}", e.getMessage());
            }
        }

        // BƯỚC 4: Báo lỗi nếu không thể định vị được địa chỉ
        log.error("[GEOCODING FATAL] Không thể tìm thấy tọa độ cho địa chỉ: {}", address);
        throw new ResourceNotFoundException("Hệ thống không thể định vị địa chỉ này. Vui lòng thử lại!");
    }

    // Chuẩn hóa chuỗi địa chỉ (xóa khoảng trắng thừa, viết thường) để làm Cache Key nhất quán
    private String normalizeAddress(String address) {
        if (address == null) return "";
        return address.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    // Hàm lưu dữ liệu Tọa độ vào Redis TTL 7 ngày
    private void saveGeoCache(String cacheKey, Double lat, Double lng) {
        try {
            redisTemplate.opsForValue().set(cacheKey, lat + "," + lng, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("[REDIS CACHE WRITE WARN] Không thể lưu cache geocoding: {}", e.getMessage());
        }
    }

    private GeocodingResponse createGeocodingResponse(Double lat, Double lng) {
        GeocodingResponse geo = new GeocodingResponse();
        geo.setLat(lat);
        geo.setLng(lng);
        return geo;
    }


    // HÀM TÍNH TIỀN CHI TIẾT
    // Công thức: Total = (BaseFare + (ExtraKm * PerKmRate)) * SurgeMultiplier
    // Tổng tiền = (Giá 2 km đầu + (Giá km sau * khoảng cách )) * hệ số nhân
    private BigDecimal computeFare(Double distanceKm, VehicleType vehicleType, Double surgeMultiplier) {
        Object redisBaseFare;
        Object redisPerKmRate;

        // Đọc Quy tắc Bảng giá từ Redis Hash (key: "pricing:rules:BIKE", ...)
        try {
            redisBaseFare = redisTemplate.opsForHash().get("pricing:rules:" + vehicleType.name(), "baseFare");
            redisPerKmRate = redisTemplate.opsForHash().get("pricing:rules:" + vehicleType.name(), "perKmRate");
        } catch (Exception e) {
            log.error("[REDIS ERROR] Lỗi khi kết nối tới Redis: {}", e.getMessage());
            throw new ResourceNotFoundException("Lỗi kết nối bộ nhớ đệm giá cước (Redis). Vui lòng thử lại sau!");
        }

        if (redisBaseFare == null || redisPerKmRate == null) {
            log.error("[PRICING ERROR] Chưa cấu hình bảng giá cho loại xe {} trong Redis!", vehicleType);
            throw new ResourceNotFoundException("Bảng giá cho loại xe " + vehicleType + " chưa được cấu hình trong Redis! Vui lòng cập nhật bảng giá trước khi đặt chuyến.");
        }

        double baseFare = Double.parseDouble(redisBaseFare.toString());
        double perKmRate = Double.parseDouble(redisPerKmRate.toString());

        // 1. Số km vượt tính từ km thứ 3 (2 km đầu đã tính trọn gói trong baseFare)
        double extraKm = Math.max(0.0, distanceKm - 2.0);

        // 2. Tổng giá trị thô
        double rawFare = (baseFare + (extraKm * perKmRate)) * surgeMultiplier;

        // 3. Làm tròn lên hàng nghìn đồng (ví dụ: 15,200đ -> 16,000đ)
        long roundedFare = (long) (Math.ceil(rawFare / 1000.0) * 1000);

        // --- LOG TOÀN BỘ THÔNG TIN VÀ CÔNG THỨC TÍNH CƯỚC ---
        log.info("========== [CHI TIẾT TÍNH GIÁ CƯỚC] ==========");
        log.info("1. Loại xe: {}", vehicleType);
        log.info("2. Quãng đường tổng: {} km | Số km vượt (trên 2km đầu): {} km", distanceKm, extraKm);
        log.info("3. Cấu hình giá từ Redis: BaseFare (2km đầu) = {} VNĐ | Đơn giá/km vượt = {} VNĐ", baseFare, perKmRate);
        log.info("4. Hệ số Surge (Tăng giá): {}x", surgeMultiplier);
        log.info("5. CÔNG THỨC: Total = (BaseFare + (ExtraKm * PerKmRate)) * Surge");
        log.info("              Total = ({} + ({} * {})) * {} = {} VNĐ", baseFare, extraKm, perKmRate, surgeMultiplier, rawFare);
        log.info("6. GIÁ SAU LÀM TRÒN: {} VNĐ", roundedFare);
        log.info("==============================================");

        return BigDecimal.valueOf(roundedFare);
    }

    // Làm tròn Tọa độ điểm đón thành Grid Zone (ví dụ: 21.03_105.78)
    // HÀM TÍNH HỆ SỐ TĂNG GIÁ DYNAMIC SURGE PRICING REALTIME
    private Double calculateDynamicSurge(Double pickupLat, Double pickupLng, VehicleType vehicleType) {
        try {
            // 1. Nhóm cụm khu vực (Grid Zone)
            double truncatedLat = Math.floor(pickupLat * 100.0) / 100.0;
            double truncatedLng = Math.floor(pickupLng * 100.0) / 100.0;
            String zoneKey = String.format(java.util.Locale.US, "%.2f_%.2f", truncatedLat, truncatedLng);

            // 2. Ưu tiên 1: Đọc Surge cấu hình thủ công bởi Admin (nếu có override)
            String manualSurge = redisTemplate.opsForValue().get("surge:zone:" + zoneKey);
            if (manualSurge != null) {
                return Double.parseDouble(manualSurge);
            }

            // 3. Ưu tiên 2: TÍNH REALTIME từ Redis GEO
            String targetVehicle = (vehicleType != null) ? vehicleType.name() : "BIKE";
            String geoKey = "drivers:available:" + targetVehicle;
            Circle searchArea = new Circle(new Point(pickupLng, pickupLat), new Distance(3.0, Metrics.KILOMETERS));

            GeoResults<RedisGeoCommands.GeoLocation<String>> availableDrivers =
                    redisTemplate.opsForGeo().radius(geoKey, searchArea);

            int driverCount = (availableDrivers != null) ? availableDrivers.getContent().size() : 0;

            // Đếm số chuyến đang được đặt trong khu vực (Demand)
            String demandStr = redisTemplate.opsForValue().get("density:demand:" + zoneKey);
            int demandCount = (demandStr != null) ? Integer.parseInt(demandStr) : 1;

            // Nếu không có tài xế rảnh nào quanh khu vực -> Trả về giá gốc 1.0x
            if (driverCount == 0) {
                log.info("[DYNAMIC SURGE] Zone {}: 0 Tài xế rảnh -> Giữ nguyên giá 1.0x", zoneKey);
                return 1.0;
            }

            // 4. Tính tỷ lệ Nhu Cầu / Số Tài Xế Rảnh
            double ratio = (double) demandCount / driverCount;

            log.info("[DYNAMIC SURGE] Zone {}: Demand = {}, Drivers = {} -> Ratio = {}", zoneKey, demandCount, driverCount, ratio);

            if (ratio >= 3.0) return 2.0; // Nhu cầu quá cao -> x2.0
            if (ratio >= 2.0) return 1.5; // Nhu cầu cao -> x1.5
            if (ratio >= 1.2) return 1.2; // Nhu cầu tăng nhẹ -> x1.2

            return 1.0; // Bình thường

        } catch (Exception e) {
            log.warn("[DYNAMIC SURGE WARN] Lỗi tính surge realtime, dùng 1.0x: {}", e.getMessage());
            return 1.0;
        }
    }

    private Map<String, Object> getRouteFromOsrm(Double pickupLat, Double pickupLng, Double dropoffLat, Double dropoffLng) {
        String url = String.format(java.util.Locale.US, "%s/%f,%f;%f,%f?overview=false",
                osrmUrl, pickupLng, pickupLat, dropoffLng, dropoffLat);
        log.info("[OSRM CALL] URL: {}", url);

        double distanceMeters = 0.0;
        double durationSeconds = 0.0;

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && "Ok".equals(response.get("code"))) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    if (route.get("distance") != null) {
                        distanceMeters = ((Number) route.get("distance")).doubleValue();
                    }
                    if (route.get("duration") != null) {
                        durationSeconds = ((Number) route.get("duration")).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error("[OSRM ERROR] Lỗi lấy tuyến đường OSRM: {}", e.getMessage());
        }

        Double distanceKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0;
        Integer durationMinutes = (int) Math.ceil(durationSeconds / 60.0);

        return Map.of("distanceKm", distanceKm, "durationMinutes", durationMinutes);
    }


}
