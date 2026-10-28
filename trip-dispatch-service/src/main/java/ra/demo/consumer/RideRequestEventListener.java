package ra.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ra.demo.client.LocationClient;
import ra.demo.constants.DriverStatus;
import ra.demo.constants.TripStatus;
import ra.demo.constants.VehicleType;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.NearbyDriverResponse;
import ra.demo.entity.Trip;
import ra.demo.event.DriverStatusEvent;
import ra.demo.event.RideRequestEvent;
import ra.demo.repository.TripRepository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RideRequestEventListener {

    private final LocationClient locationClient;
    private final StringRedisTemplate redisTemplate;
    private final TripRepository tripRepository;

    // Inject thêm KafkaTemplate để đẩy sự kiện sang WebSocket Gateway
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Tạo Scheduler Pool để quản lý timer 30s
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @KafkaListener(topics = "trip-dispatch-topic", groupId = "trip-dispatch-group")
    public void handleRideRequest(RideRequestEvent event) {
        if (event == null || event.getTripId() == null) return;

        // 1. Quét tài xế rảnh trong bán kính 5km
        ApiResponse<List<NearbyDriverResponse>> response = locationClient.findNearbyDrivers(
        event.getPickupLat(),
                event.getPickupLng(),
                5.0,
                event.getVehicleType()
        );

        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            List<NearbyDriverResponse> nearbyDrivers = response.getData();

            // 2. GHI LOG RÕ BẠN ĐANG CÓ BAO NHIÊU TÀI XẾ XUNG QUANH
            log.info("[DISPATCH MATCH] Quét thấy TỔNG CỘNG {} tài xế rảnh xung quanh điểm đón của Trip #{}",
                    nearbyDrivers.size(), event.getTripId());

            // Lấy danh sách ID tài xế (Đã được sắp xếp từ gần nhất đến xa nhất nhờ Redis GEO)
            List<String> driverIds = nearbyDrivers.stream()
                    .map(d -> String.valueOf(d.getDriverId()))
                    .toList();

            // 3. LOG CHI TIẾT TÀI XẾ THỨ NHẤT (ƯU TIÊN 1)
            NearbyDriverResponse firstDriver = nearbyDrivers.get(0);
            log.info("[DISPATCH FIRST CHOICE] Lựa chọn ƯU TIÊN THỨ NHẤT: Tài xế #{} (Khoảng cách: {} km)",
                    firstDriver.getDriverId(), firstDriver.getDistanceKm());

            // 4. Lưu toàn bộ danh sách tài xế vào Redis Queue
            String queueKey = "trip:dispatch:queue:" + event.getTripId();
            redisTemplate.opsForList().rightPushAll(queueKey, driverIds);
            redisTemplate.expire(queueKey, Duration.ofMinutes(5)); // Hạn chót cho cả quá trình dispatch

            // 5. Bắt đầu gửi đề nghị cho Tài xế thứ nhất
            dispatchToNextDriver(event.getTripId(),event);
        } else {
            log.warn("[DISPATCH] Không có tài xế nào xung quanh cho Trip #{}", event.getTripId());
        }
    }

    public void dispatchToNextDriver(Long tripId, RideRequestEvent event) {
        String queueKey = "trip:dispatch:queue:" + tripId;

        // Pop (Lấy ra) tài xế tiếp theo đứng đầu hàng đợi
        String nextDriverId = redisTemplate.opsForList().leftPop(queueKey);

        if (nextDriverId != null) {
            // Đánh dấu tài xế đang giữ offer này trong 30 giây
            String currentOfferKey = "trip:dispatch:current:" + tripId;
            redisTemplate.opsForValue().set(currentOfferKey, nextDriverId, Duration.ofSeconds(30));

            log.info("[DISPATCH SEQUENTIAL] Đang gửi đề nghị Trip #{} DUY NHẤT cho Tài xế #{}. Hạn phản hồi: 30 giây",
                    tripId, nextDriverId);

            // Tìm thông tin Trip từ Database để lấy thông tin điểm đón/trả và giá tiền
            Trip tripInfo = tripRepository.findById(tripId).orElse(null);

            RideRequestEvent offerEvent = RideRequestEvent.builder()
                    .tripId(tripId)
                    .candidateDriverIds(List.of(nextDriverId)) // BẮT BUỘC: Đánh dấu đích danh ID tài xế nhận chuyến!
                    .customerId(tripInfo != null ? tripInfo.getCustomerId() : null)
                    .pickupAddress(tripInfo != null ? tripInfo.getPickupAddress() : null)
                    .pickupLat(event != null ? event.getPickupLat() : null)
                    .pickupLng(event != null ? event.getPickupLng() : null)
                    .dropoffAddress(tripInfo != null ? tripInfo.getDropoffAddress() : null)
                    .dropoffLat(event != null ? event.getDropoffLat() : null)
                    .dropoffLng(event != null ? event.getDropoffLng() : null)
                    .amount(tripInfo != null ? tripInfo.getFareAmount() : null)
                    .vehicleType(event != null ? event.getVehicleType() : "BIKE")
                    .paymentMethod(event != null ? event.getPaymentMethod() : "CASH")
                    .build();

            // Bắn event lên Kafka topic "ride-request-topic"
            kafkaTemplate.send("ride-request-topic", String.valueOf(tripId), offerEvent);
            log.info("[DISPATCH PUSH] Đã bắn Event Kafka đề nghị Trip #{} tới Tài xế #{}", tripId, nextDriverId);

            // Timer đếm ngược 30s tự động chuyển sang tài xế tiếp theo nếu không phản hồi
            scheduler.schedule(() -> {
                // Kiểm tra trong DB xem Chuyến đi có còn ở trạng thái Requested (chưa ai nhận) không
                Trip trip = tripRepository.findById(tripId).orElse(null);

                if (trip != null && trip.getStatus() == TripStatus.Requested) {
                    log.info("[DISPATCH TIMEOUT] Tài xế #{} không phản hồi Trip #{} sau 30s. Đang chuyển tài xế tiếp theo...",
                            nextDriverId, tripId);

                    // 1. Tạo Key phạt trong Redis với TTL = 30 giây (Chặn tài xế bấm bật lại ONLINE)
                    String penaltyKey = "driver:penalty:" + nextDriverId;
                    redisTemplate.opsForValue().set(penaltyKey, "TIMEOUT_LOCK", Duration.ofSeconds(30));

                    // 2. Bắn Event ép tài xế về trạng thái OFFLINE sang Kafka "driver-status-topic"
                    // Location Service sẽ nhận Event này và tự động gạt tài xế khỏi Redis GEO drivers:available:*
                    String vehicleTypeStr = event != null && event.getVehicleType() != null ? event.getVehicleType() : "BIKE";
                    DriverStatusEvent penaltyEvent = DriverStatusEvent.builder()
                            .driverId(Long.parseLong(nextDriverId))
                            .status(DriverStatus.OFFLINE)
                            .vehicleType(VehicleType.valueOf(vehicleTypeStr))
                            .build();

                    kafkaTemplate.send("driver-status-topic", nextDriverId, penaltyEvent);

                    // 3. Xóa offer hiện tại trong Redis
                    redisTemplate.delete("trip:dispatch:current:" + tripId);

                    // 4. Chuyển sang tài xế kế tiếp trong Queue
                    dispatchToNextDriver(tripId,event);
                }
            }, 30, TimeUnit.SECONDS);

        } else {
            log.warn("[DISPATCH FINISHED] Đã hỏi toàn bộ tài xế trong danh sách nhưng không ai nhận Trip #{}. Chuyến xe tiếp tục treo để chờ tài xế mới.", tripId);
            // Cập nhật trip status = NO_DRIVER_FOUND hoặc thông báo lại cho Khách hàng
        }
    }
}