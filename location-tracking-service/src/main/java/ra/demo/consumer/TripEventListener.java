package ra.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ra.demo.constants.TripStatus;
import ra.demo.event.TripStatusEvent;
import ra.demo.service.LocationService;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripEventListener {

    private final LocationService locationService;

    /**
     * Hàm này lắng nghe loa thông báo "trip-status-topic" (do Trip Service phát ra).
     * Mọi biến động của chuyến đi (Khách đặt, Tài xế nhận, Hủy chuyến, Hoàn thành) đều chạy vào đây.
     */
    @KafkaListener(topics = "trip-status-topic", groupId = "location-tracking-group")
    public void handleTripStatusChange(TripStatusEvent event) {
        if (event == null || event.getDriverId() == null || event.getTripStatus() == null) return;

        TripStatus status = event.getTripStatus();
        Long driverId = event.getDriverId();

        // 1. Khi Tài xế bấm "ĐỒNG Ý NHẬN CHUYẾN" (Accepted)
        if (status == TripStatus.Accepted || "ACCEPTED".equalsIgnoreCase(status.name())) {
            // Lập tức XÓA TÊN tài xế khỏi danh sách xe rảnh để người khác không tìm thấy/không phát thêm chuyến nữa!
            locationService.removeDriverLocation(driverId);
            log.info("[KAFKA REMOVE] Tài xế #{} đã nhận chuyến #{}. Đã XÓA khỏi Redis GEO!", driverId, event.getTripId());
        }

        // 2. Khi Chuyến đi BỊ HỦY (Cancelled)
        else if (status == TripStatus.Cancelled || "CANCELLED".equalsIgnoreCase(status.name())) {
            log.info("[KAFKA CANCEL] Chuyến đi #{} bị hủy. Xử lý lại trạng thái cho Tài xế #{}", event.getTripId(), driverId);
            // Lúc này tùy logic app, tài xế rảnh lại thì lượt gửi GPS tiếp theo sẽ tự động đưa họ về lại Redis GEO.
        }
    }
}