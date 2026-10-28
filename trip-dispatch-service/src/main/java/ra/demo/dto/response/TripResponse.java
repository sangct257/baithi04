package ra.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.PaymentMethod;
import ra.demo.constants.TripStatus;
import ra.demo.constants.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TripResponse {
    private Long tripId; // mã chuyến
    private Long customerId; // mã khách hàng
    private Long driverId; // mã tài xế
    private String pickupAddress; // địa điểm đón
    private String dropoffAddress; // địa điêểm trả
    private VehicleType vehicleType; // loại xe (BIKE, CAR_4_SEATS, CAR_7_SEATS)
    private Double distanceKm; // Khoang cach (km)
    private Integer durationMinutes; // Thời gian dự kiến (phut)
    private BigDecimal fareAmount; // Tien cuc (VND)
    private PaymentMethod paymentMethod; // Hinh thuc thanh toan (CASH, WALLET)
    private String paymentStatus; // Trang thai thanh toan (UNPAID, PAID) (Chưa thanh toán, Đã thanh toán)
    private TripStatus tripStatus; // Trang thai chuyen (REQUESTED, ACCEPTED, ARRIVED, IN_PROGRESS, COMPLETED, CANCELLED) -> (Đặt Xe , Nhận đơn , Đến điểm đón , Bắt đầu chuyến , Hoàn thành , Huỷ)
    private String cancelReason; // Lý do huỷ quốc
    private String cancelledBy; // Huỷ bởi CUSTOMER/DRIVER
    private LocalDateTime createdAt; // Thời gian tạo
    private LocalDateTime updatedAt;

}
