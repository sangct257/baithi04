package ra.demo.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ra.demo.event.DriverLocationStreamEvent;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LocationWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    /**
     * App Tài xế gọi Endpoint STOMP này định kỳ 3 - 5 giây/lần
     * Destination từ Mobile App: /app/location/stream
     */
    @MessageMapping("/location/stream")
    public void receiveDriverLocationStream(DriverLocationStreamEvent event) {
        // Validation dữ liệu đầu vào
        if (event == null || event.getDriverId() == null || event.getLat() == null || event.getLng() == null) {
            log.warn("[WS STREAM WARN] Nhận gói tin GPS rỗng hoặc thiếu thông tin tọa độ");
            return;
        }

        log.debug("[WS GPS STREAM 3s] Tài xế #{}: ({}, {}), TripId={}",
                event.getDriverId(), event.getLat(), event.getLng(), event.getTripId());

        // BƯỚC 1: Bắn bất đồng bộ sang Kafka để Location Service lưu Redis GEO & Hash
        try {
            String driverKey = String.valueOf(event.getDriverId());
            kafkaTemplate.send("driver-location-telemetry", driverKey, event);
        } catch (Exception e) {
            log.error("[KAFKA PUSH ERROR] Lỗi không thể đẩy GPS của Tài xế #{} vào Kafka", event.getDriverId(), e);
        }

        // BƯỚC 2: Nếu Tài xế đang trong chuyến đi (tripId != null),
        // Forward vị trí GPS realtime trực tiếp tới App Khách hàng để vẽ icon xe chạy trên bản đồ
        if (event.getTripId() != null) {
            String customerTopic = "/topic/trip/" + event.getTripId() + "/location";
            messagingTemplate.convertAndSend(customerTopic, event);
        }
    }
}
