package ra.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import ra.demo.event.TripStatusEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripStatusEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "trip-status-topic", groupId = "websocket-gateway-status-group")
    public void handleTripStatusChange(TripStatusEvent event) {
        if (event == null || event.getTripId() == null) return;

        // 1. PUSH TẤT CẢ THAY ĐỔI TRẠNG THÁI (ACCEPTED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED) TỚI KHÁCH HÀNG
        String customerTopic = "/topic/trip/" + event.getTripId() + "/status";
        messagingTemplate.convertAndSend(customerTopic, event);
        log.info("[WS STATUS PUSH] Trip #{} đổi trạng thái thành [{}] -> Bắn tới kênh Khách: {}",
                event.getTripId(), event.getTripStatus(), customerTopic);


        // 2. XỬ LÝ TRƯỜNG HỢP HỦY CHUYẾN 2 CHIỀU (CANCELLED)
        if ("CANCELLED".equalsIgnoreCase(String.valueOf(event.getTripStatus()))) {

            // TH1: KHÁCH HÀNG BẤM HỦY CHUYẾN -> Bắn lệnh thu hồi nổ chuyến / tắt popup nhận đơn trên App Tài xế
            if ("CUSTOMER".equalsIgnoreCase(event.getCancelledBy()) && event.getDriverId() != null) {
                String driverTopic = "/topic/driver/" + event.getDriverId() + "/ride-request";

                Map<String, Object> cancelPayload = Map.of(
                        "tripId", event.getTripId(),
                        "status", "CANCELLED",
                        "message", "Chuyến đi đã bị Khách hàng hủy bỏ!",
                        "cancelReason", event.getCancelReason() != null ? event.getCancelReason() : "Khách hàng hủy chuyến"
                );

                messagingTemplate.convertAndSend(driverTopic, (Object) cancelPayload);
                log.info("[WS CUSTOMER CANCEL] Đã gửi thông báo HỦY CHUYẾN tới Tài xế #{}", event.getDriverId());
            }

            // TH2: TÀI XẾ BẤM HỦY CHUYẾN -> Thông báo cho Khách hàng biết Lý do & Hệ thống tiếp tục tìm xe khác
            else if ("DRIVER".equalsIgnoreCase(event.getCancelledBy())) {
                Map<String, Object> cancelPayload = Map.of(
                        "tripId", event.getTripId(),
                        "status", "DRIVER_CANCELLED",
                        "message", "Tài xế đã hủy chuyến đi. Hệ thống đang tiến hành tìm kiếm tài xế khác cho bạn!",
                        "cancelReason", event.getCancelReason() != null ? event.getCancelReason() : "Tài xế hủy chuyến"
                );

                messagingTemplate.convertAndSend(customerTopic, (Object) cancelPayload);
                log.info("[WS DRIVER CANCEL] Đã gửi thông báo Tài xế HỦY CHUYẾN tới Khách hàng của Trip #{}", event.getTripId());
            }
        }
    }
}