package ra.demo.service.impl;

import com.uber.h3core.H3Core;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ra.demo.constants.VehicleType;
import ra.demo.dto.request.DriverLocationRequest;
import ra.demo.dto.response.NearbyDriverResponse;
import ra.demo.security.utils.SecurityUtils;
import ra.demo.service.LocationService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {
    private final StringRedisTemplate redisTemplate;
    private H3Core h3;

    /**
     * HÀM 1: Cập nhật GPS liên tục do App Tài xế bắn về (Thường 3s - 5s gửi 1 lần)
     */
    @Override
    public void updateDriverLocation(DriverLocationRequest request) {
        // Lấy ID tài xế từ request hoặc trích xuất từ Token JWT
        Long driverId = request.getDriverId();
        if (driverId == null) {
            try {
                driverId = SecurityUtils.getCurrentUserId();
            } catch (Exception e) {
                log.warn("[LOCATION UPDATE WARNING] Không tìm thấy driverId trong Request lẫn Security Context: {}", e.getMessage());
                return;
            }
        }

        if (driverId == null) {
            log.warn("[LOCATION UPDATE WARNING] Không tìm thấy driverId để cập nhật vị trí");
            return;
        }

        String driverIdStr = String.valueOf(driverId);
        VehicleType vehicleType = request.getVehicleType();

        // TỰ ĐỘNG XÁC ĐỊNH VEHICLE_TYPE NẾU REQUEST KHÔNG TRUYỀN
        if (vehicleType == null) {
            // Cách A: Đọc từ Redis Profile (Nơi DriverStatusEventListener lưu khi tài xế ON)
            Object cachedVehicle = redisTemplate.opsForHash().get("driver:profile:" + driverIdStr, "vehicleType");
            if (cachedVehicle != null && !cachedVehicle.toString().isBlank()) {
                vehicleType = VehicleType.valueOf(cachedVehicle.toString());
            }
            else {
                // Cách B: Đọc từ Redis Location lịch sử trước đó
                Object lastSavedVehicle = redisTemplate.opsForHash().get("driver:location:" + driverIdStr, "vehicleType");
                if (lastSavedVehicle != null && !lastSavedVehicle.toString().isBlank()) {
                    vehicleType = VehicleType.valueOf(lastSavedVehicle.toString());
                } else {
                    // Fallback mặc định nếu hoàn toàn chưa có thông tin
                    vehicleType = VehicleType.BIKE;
                }
            }
        }

        // BƯỚC 1: Đánh dấu Lịch sử vị trí mới nhất (Dạng Redis Hash: "driver:location:{driverId}")
        String lastLocationKey = "driver:location:" + driverIdStr;
        redisTemplate.opsForHash().put(lastLocationKey, "lat", String.valueOf(request.getLatitude()));
        redisTemplate.opsForHash().put(lastLocationKey, "lng", String.valueOf(request.getLongitude()));
        redisTemplate.opsForHash().put(lastLocationKey, "vehicleType", vehicleType.name());

        // BƯỚC 2: Kiểm tra trạng thái hoạt động chính xác từ Redis Cache
        String status = redisTemplate.opsForValue().get("driver:status:" + driverIdStr);
        String geoKey = "drivers:available:" + vehicleType.name();

        // ĐIỀU KIỆN CHẶT CHẼ: CHỈ KHI TRẠNG THÁI CHÍNH XÁC LÀ "ONLINE" MỚI ĐƯỢC COI LÀ XE RẢNH
        if (status == null || "ONLINE".equalsIgnoreCase(status)) {
            redisTemplate.opsForGeo().add(
                    geoKey,
                    new Point(request.getLongitude(), request.getLatitude()),
                    driverIdStr
            );
            log.debug("[LOCATION ONLINE] Cập nhật GPS Tài xế #{}: ({}, {})", driverId, request.getLatitude(), request.getLongitude());
        }
        // Nếu OFFLINE, BUSY hoặc null (Chưa bật Online) -> Đảm bảo loại ra khỏi tập hợp xe rảnh trên Redis GEO
        else {
            redisTemplate.opsForZSet().remove(geoKey, driverIdStr);
            log.debug("[LOCATION NOT_AVAILABLE] Tài xế #{} đang {}. Xóa khỏi {}", driverId, status, geoKey);
        }
    }


    /**
     * HÀM 2: Tìm kiếm các tài xế xung quanh vị trí của Khách hàng (Truy vấn không gian)
     */
    @Override
    public List<NearbyDriverResponse> findNearbyDrivers(Double lat, Double lng, Double radiusKm, String vehicleType) {
        // Mặc định nếu không chọn loại xe thì lấy Xe Máy (BIKE)
        String targetVehicleType = (vehicleType != null && !vehicleType.isBlank()) ? vehicleType : "BIKE";
        String geoKey = "drivers:available:" + targetVehicleType;

        // Vẽ một hình tròn quanh vị trí Khách hàng (Tọa độ Lat/Lng của khách, Bán kính X km)
        Circle searchArea = new Circle(
                new Point(lng, lat),
                new Distance(radiusKm, Metrics.KILOMETERS)
        );

        // Cấu hình kết quả trả về: Kèm khoảng cách, Kèm tọa độ, Sắp xếp từ GẦN NHẤT đến XA NHẤT
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance() // Khoảng cách
                .includeCoordinates() // Toạ độ
                .sortAscending(); // Ưu tiên tài xế gần nhất

        // Bắt đầu "Quét" Redis GEO trong hình tròn đó!
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(geoKey, searchArea, args);

        List<NearbyDriverResponse> nearbyDrivers = new ArrayList<>();

        // Lặp qua từng tài xế tìm thấy được
        if (results != null) {
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
                String driverIdStr = result.getContent().getName();

                if (driverIdStr == null || driverIdStr.isBlank() || "null".equalsIgnoreCase(driverIdStr.trim())) {
                    log.warn("[SPATIAL QUERY WARNING] Phát hiện memberName không hợp lệ trong Redis GEO: '{}'", driverIdStr);
                    continue;
                }

                try {
                    // Làm sạch ID tài xế (Lấy con số ID)
                    String cleanDriverIdStr = driverIdStr.contains(":")
                            ? driverIdStr.substring(driverIdStr.lastIndexOf(":") + 1)
                            : driverIdStr;

                    Long driverId = Long.parseLong(cleanDriverIdStr.trim());
                    Point point = result.getContent().getPoint();
                    Double distance = result.getDistance().getValue();

                    // Đóng gói thông tin tài xế (ID, Vĩ độ, Kinh độ, Khoảng cách km) gửi lại cho Client
                    nearbyDrivers.add(NearbyDriverResponse.builder()
                            .driverId(driverId)
                            .latitude(point != null ? point.getY() : null)
                            .longitude(point != null ? point.getX() : null)
                            .distanceKm(Math.round(distance * 100.0) / 100.0)
                            .vehicleType(VehicleType.valueOf(targetVehicleType.toUpperCase()))
                            .build());

                } catch (Exception e) {
                    log.error("[SPATIAL QUERY ERROR] Không thể parse driverId từ chuỗi: '{}'", driverIdStr, e);
                }
            }
        }

        log.info("[SPATIAL QUERY] Tìm thấy {} tài xế loại [{}] xung quanh bán kính {} km",
                nearbyDrivers.size(), targetVehicleType, radiusKm);
        return nearbyDrivers;
    }

    /**
     * HÀM 3: Xóa tài xế khỏi toàn bộ danh sách xe rảnh (Khi bận chở khách hoặc Offline)
     */
    @Override
    public void removeDriverLocation(Long driverId) {
        String driverIdStr = String.valueOf(driverId);

        // Duyệt qua tất cả các key loại xe và xóa ID tài xế này ra khỏi Redis ZSet/GEO
        for (VehicleType type : VehicleType.values()) {
            String geoKey = "drivers:available:" + type.name();
            redisTemplate.opsForZSet().remove(geoKey, driverIdStr);
        }

        log.info("[LOCATION REMOVE] Đã xóa Tài xế #{} khỏi toàn bộ danh sách xe rảnh", driverId);
    }

    @PostConstruct
    public void initH3(){
        try {
            this.h3 = H3Core.newInstance();
        } catch (Exception e) {
            log.error("[H3 INIT ERROR] Không thể khởi tạo Uber H3 Core: {}", e.getMessage());
        }
    }

    @Override
    public String getH3Index(Double lat, Double lng) {
        if (h3 != null && lat != null && lng != null) {
            // Resolution 8 tương đương ô lục giác ~0.73 km2
            return h3.geoToH3Address(lat, lng, 8);
        }
        return null;
    }
}
