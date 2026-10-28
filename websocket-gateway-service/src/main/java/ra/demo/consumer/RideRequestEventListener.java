package ra.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ra.demo.event.RideRequestEvent;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RideRequestEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    // Lắng nghe Event nổ chuyến 30s được bắn từ Trip Service qua Kafka
    @KafkaListener(
            topics = "ride-request-topic",
            groupId = "websocket-gateway-group"
    )
    public void handleRideRequest(RideRequestEvent event) {
        if (event == null || event.getTripId() == null) {
            log.warn("[DISPATCH WARN] Nhận Event Trip null hoặc thiếu TripId");
            return;
        }

        log.info("[KAFKA CONSUME] Nhận thông báo Trip #{}, Khách #{}, Đón tại ({}, {})",
                event.getTripId(), event.getCustomerId(), event.getPickupLat(), event.getPickupLng());

        // Kiểm tra Null Safety cho Tọa độ GPS trước khi tạo Point GEO
        if (event.getPickupLat() == null || event.getPickupLng() == null) {
            log.error("[DISPATCH ERROR] Trip #{} thiếu tọa độ điểm đón! Bỏ qua nổ chuyến.", event.getTripId());
            return;
        }

        // 1. Lấy danh sách ID các tài xế ĐÃ TỪ CHỐI hoặc ĐỂ TRÔI chuyến này trước đó từ Redis Set
        String rejectedKey = "trip:rejected:" + event.getTripId();
        Set<String> rejectedDriverIds = redisTemplate.opsForSet().members(rejectedKey);

        // 2. Quét Redis GEO tìm các tài xế RẢNH trong bán kính 3km quanh điểm đón
        String vehicleType = event.getVehicleType() != null ? event.getVehicleType() : "BIKE";
        String geoKey = "drivers:available:" + vehicleType;

        Circle searchArea = new Circle(
                new Point(event.getPickupLng(), event.getPickupLat()), // Lng (Kinh độ) trước, Lat (Vĩ độ) sau
                new Distance(3.0, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(geoKey, searchArea);

        if (results == null || results.getContent().isEmpty()) {
            log.warn("[DISPATCH WARN] Không tìm thấy tài xế rảnh nào quanh Trip #{} (Bán kính 3km)", event.getTripId());
            return;
        }

        int pushedCount = 0;

        // 3. Lặp qua danh sách tài xế quét được từ Redis GEO
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            String driverIdStr = result.getContent().getName();

            if (driverIdStr == null || driverIdStr.isBlank() || "null".equalsIgnoreCase(driverIdStr.trim())) {
                continue;
            }

            // Làm sạch ID tài xế (Lấy con số ID)
            String cleanDriverId = driverIdStr.contains(":")
                    ? driverIdStr.substring(driverIdStr.lastIndexOf(":") + 1)
                    : driverIdStr;

            // BỎ QUA nếu tài xế này đã Từ chối / Trôi chuyến này ở vòng trước
            if (rejectedDriverIds != null && (rejectedDriverIds.contains(cleanDriverId) || rejectedDriverIds.contains(driverIdStr))) {
                log.info("[SKIP DRIVER] Bỏ qua Tài xế #{} vì đã từ chối/trôi chuyến Trip #{} trước đó", cleanDriverId, event.getTripId());
                continue;
            }

            // Bắn WebSocket thông báo nổ chuyến tới App tài xế hợp lệ
            String destination = "/topic/driver/" + cleanDriverId + "/ride-request";
            messagingTemplate.convertAndSend(destination, event);
            pushedCount++;

            log.info("[PUSH SUCCESS] Đã nổ chuyến Trip #{} tới Tài xế Rảnh #{} qua {}",
                    event.getTripId(), cleanDriverId, destination);
        }

        log.info("[DISPATCH FINISHED] Đã nổ chuyến Trip #{} cho tổng cộng {} tài xế hợp lệ",
                event.getTripId(), pushedCount);
    }
}