package ra.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ra.demo.constants.VehicleType;
import ra.demo.dto.request.DriverLocationRequest;
import ra.demo.entity.DriverLocationHistory;
import ra.demo.event.DriverLocationStreamEvent;
import ra.demo.repository.DriverLocationHistoryRepository;
import ra.demo.service.LocationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service lắng nghe luồng định vị GPS (Telemetry Consumer) chịu tải cao.
 * Nhiệm vụ:
 * 1. Cập nhật vị trí Realtime vào Redis (Tốc độ < 2ms để tìm xe gần nhất).
 * 2. Lọc rác GPS và Gom lô (Batching) lưu lịch sử vào PostgreSQL/PostGIS.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DriverLocationTelemetryListener {

    private final LocationService locationService;
    private final DriverLocationHistoryRepository historyRepository;

    // Hàng đợi đệm In-Memory trên RAM (Thread-safe) để chứa tạm GPS trước khi gom lô ghi vào Database.
    // Dùng ConcurrentLinkedQueue giúp nhiều luồng Kafka thêm dữ liệu vào cùng lúc mà không bị nghẽn (Non-blocking).
    private final ConcurrentLinkedQueue<DriverLocationHistory> batchBuffer = new ConcurrentLinkedQueue<>();

    // Map lưu tạm tọa độ GPS vừa được duyệt lưu DB gần nhất của từng tài xế.
    // Mục đích: So sánh khoảng cách di chuyển để lọc xe đứng im (tránh ghi dữ liệu trùng lặp).
    private final Map<Long, LastKnownLocation> lastSavedLocations = new ConcurrentHashMap<>();

    private static class LastKnownLocation {
        double lat;
        double lng;

        LastKnownLocation(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }


    /**
     * Lắng nghe stream tọa độ GPS do ứng dụng tài xế gửi lên (tần suất 3s - 5s / lần) từ Kafka Topic.
     */
    @KafkaListener(
            topics = "driver-location-telemetry",
            groupId = "location-tracking-group"
    )
    public void handleDriverLocationStream(DriverLocationStreamEvent event) {
        if (event == null || event.getDriverId() == null) return;

        log.debug("📥 [KAFKA TELEMETRY] Nhận GPS Tài xế #{}: Lat={}, Lng={}",
                event.getDriverId(), event.getLat(), event.getLng());

        // =========================================================================
        // 1. LUỒNG NÓNG (HOT PATH): Cập nhật vị trí Realtime vào Redis GEO
        // Mục đích: Phục vụ khách hàng quét xe rảnh trong bán kính gần nhất (< 2ms)
        // =========================================================================
        DriverLocationRequest request = DriverLocationRequest.builder()
                .driverId(event.getDriverId())
                .latitude(event.getLat())
                .longitude(event.getLng())
                .vehicleType(event.getVehicleType() != null ? VehicleType.valueOf(event.getVehicleType()) : VehicleType.BIKE)
                .build();

        // Ghi đè tọa độ mới nhất vào Redis Hash & Redis GEO
        locationService.updateDriverLocation(request);

        // =========================================================================
        // 2. LUỒNG LẠNH (COLD PATH): Lọc xe đứng im & Đẩy vào hàng đợi đệm RAM
        // Mục đích: Chuẩn bị dữ liệu để Batch Insert lưu lịch sử chạy xe xuống PostgreSQL
        // =========================================================================
        Long driverId = event.getDriverId();
        double newLat = event.getLat();
        double newLng = event.getLng();

        LastKnownLocation lastLocation = lastSavedLocations.get(driverId);

        // BỘ LỌC KHẢO SÁT KHOẢNG CÁCH:
        // Chỉ chấp nhận lưu DB nếu tài xế di chuyển từ 10 mét trở lên so với vị trí đã lưu trước đó.
        if (lastLocation == null || isMovedFarEnough(lastLocation.lat, lastLocation.lng, newLat, newLng, 10.0)) {

            // Cập nhật mốc tọa độ mới đã duyệt vào Cache RAM
            lastSavedLocations.put(driverId, new LastKnownLocation(newLat, newLng));

            // Mã hóa tọa độ (Lat, Lng) thành Mã ô lục giác Uber H3 Index (độ phân giải 8 ~ 0.73km2)
            String h3Address = locationService.getH3Index(newLat, newLng);

            DriverLocationHistory history = DriverLocationHistory.builder()
                    .driverId(driverId)
                    .tripId(event.getTripId()) // Lưu ID chuyến đi (Có giá trị nếu đang chở khách, null nếu đang rảnh)
                    .latitude(newLat)
                    .longitude(newLng)
                    .vehicleType(event.getVehicleType())
                    .h3Index(h3Address)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Ném bản ghi hợp lệ vào hàng đợi đệm RAM (Mất 0.001ms, chưa đụng tới DB)
            batchBuffer.add(history);
        } else {
            log.debug("[GPS IDLE IGNORED] Tài xế #{} đứng im (< 10m). Không cho vào đệm DB.", driverId);
        }

        // TỐI ƯU SỐ LƯỢNG: Nếu hàng đợi đệm tích lũy đủ 200 bản ghi -> Xả đệm ghi xuống DB ngay lập tức
        if (batchBuffer.size() >= 200) {
            flushBatchToDb();
        }
    }

    /**
     * LỊCH TRÌNH ĐỊNH KỲ 1 (Timer 5 giây):
     * Cứ mỗi 5 giây, kiểm tra hàng đợi đệm. Nếu có dữ liệu sẽ xả 1 lần xuống Database.
     * Tránh việc để dữ liệu nằm trong RAM quá lâu nếu chưa đủ 200 bản ghi.
     */
    @Scheduled(fixedRate = 5000)
    public void scheduledFlush() {
        if (!batchBuffer.isEmpty()) {
            flushBatchToDb();
        }
    }

    /**
     * LỊCH TRÌNH ĐỊNH KỲ 2 (Chính sách lưu trữ / Clean-up 2h sáng):
     * Mỗi ngày vào lúc 2:00 AM, tự động quét và xóa sạch lịch sử GPS cũ hơn 30 ngày.
     * Giúp ổ cứng máy tính không bao giờ bị cạn kiệt dung lượng.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCleanup() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        int deletedCount = historyRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("[DATA RETENTION] Đã xóa {} bản ghi GPS cũ hơn 30 ngày", deletedCount);
    }

    /**
     * Hàm rút toàn bộ dữ liệu đang chờ trong đệm RAM và gọi saveAll() để ghi 1 câu lệnh Batch Insert xuống PostgreSQL.
     */
    private synchronized void flushBatchToDb() {
        if (batchBuffer.isEmpty()) return; // Tránh tạo List rỗng vô ích

        List<DriverLocationHistory> recordsToSave = new ArrayList<>();
        while (!batchBuffer.isEmpty()) {
            recordsToSave.add(batchBuffer.poll());
        }

        if (!recordsToSave.isEmpty()) {
            try {
                historyRepository.saveAll(recordsToSave);
                log.info("💾 [POSTGIS BATCH WRITE] Đã lưu thành công lô {} bản ghi GPS vào PostgreSQL", recordsToSave.size());
            } catch (Exception e) {
                log.error("[POSTGIS BATCH ERROR] Lỗi khi batch insert GPS", e);
                // Phương án dự phòng nếu lỗi DB: đẩy ngược lại queue để thử lại sau
                batchBuffer.addAll(recordsToSave);
            }
        }
    }

    /**
     * Công thức toán học Haversine: Tính khoảng cách đường cong mặt cầu giữa 2 tọa độ GPS (Đơn vị: Mét).
     * Trả về true nếu khoảng cách >= minMeters (10m).
     */
    private boolean isMovedFarEnough(double lat1, double lon1, double lat2, double lon2, double minMeters) {
        final int R = 6371000; // Bán kính Trái Đất (Mét)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (R * c) >= minMeters;
    }
}
