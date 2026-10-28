package ra.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ra.demo.constants.VehicleType;
import ra.demo.event.DriverStatusEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DriverStatusEventListener {
    // CHỈ DÙNG StringRedisTemplate ĐỂ QUẢN LÝ CACHE / REDIS GEO TRONG LOCATION SERVICE
    private final StringRedisTemplate redisTemplate;

    /**
     * @KafkaListener: Hàm này giống như người đứng gác.
     * Hễ có tin nhắn gửi vào loa thông báo "driver-status-topic" (do User Service phát ra),
     * hàm này sẽ tự động nhảy vào xử lý!
     */
    @KafkaListener(topics = "driver-status-topic", groupId = "location-tracking-group")
    public void handleDriverStatusChange(DriverStatusEvent event) {
        // Nếu tin nhắn rỗng hoặc không có ID tài xế thì bỏ qua, không làm gì cả
        if (event == null || event.getDriverId() == null) return;

        String driverIdStr = String.valueOf(event.getDriverId());
        String redisKey = "driver:status:" + driverIdStr;
        String profileKey = "driver:profile:" + driverIdStr;
        String statusStr = event.getStatus() != null ? event.getStatus().name() : "OFFLINE";

        // Cập nhật trạng thái chính vào Redis
        redisTemplate.opsForValue().set(redisKey, statusStr);
        log.info("[KAFKA CONSUME] Đã cập nhật trạng thái Tài xế #{}: {}", driverIdStr, statusStr);

        // BƯỚC 1: Nếu BẬT ONLINE -> Chỉ cache Profile & Loại xe (Chờ có Stream GPS thực tế mới push vào GEO)
        if ("ONLINE".equalsIgnoreCase(statusStr)) {
            // Lấy VehicleType từ Event (nếu Event có sẵn), nếu không thì mặc định là BIKE
            String vehicleTypeStr = (event.getVehicleType() != null)
                    ? event.getVehicleType().name()
                    : VehicleType.BIKE.name();

            redisTemplate.opsForHash().put(profileKey, "status", "ONLINE");
            redisTemplate.opsForHash().put(profileKey, "vehicleType", vehicleTypeStr);
            log.info("[KAFKA ONLINE PROFILE] Đã sẵn sàng Profile cho Tài xế #{}. Chờ gói tin GPS realtime đầu tiên!", driverIdStr);
        }
        // BƯỚC 2: Nếu OFFLINE hoặc BUSY -> Xóa ngay lập tức khỏi Redis GEO
        else if ("OFFLINE".equalsIgnoreCase(statusStr)) {
            // Quét và xóa tài xế khỏi tất cả tập hợp Redis GEO xe rảnh
            for (VehicleType type : VehicleType.values()) {
                String geoKey = "drivers:available:" + type.name();
                redisTemplate.opsForZSet().remove(geoKey, driverIdStr);
            }
            log.info("[KAFKA REMOVE] Tài xế #{} chuyển {}. Đã dọn dẹp Cache và xóa khỏi Redis GEO xe rảnh!", driverIdStr, statusStr);
        }

    }
}