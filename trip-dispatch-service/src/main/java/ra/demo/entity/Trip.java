package ra.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import ra.demo.constants.PaymentMethod;
import ra.demo.constants.TripStatus;
import ra.demo.constants.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Thông tin định danh ---
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "driver_id")
    private Long driverId; // Null khi mới tạo (REQUESTED)

    // --- Địa điểm đón / trả ---
    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress; // Tên địa chỉ điểm đón

    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress; // Tên địa chỉ điểm trả

    // --- Thông tin chuyến đi ---
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType; // BIKE, CAR_4_SEATS, CAR_7_SEATS

    @Column(name = "distance_km")
    private Double distanceKm; // Khoảng cách (km)

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // Thời gian dự kiến (phút)

    // --- Giá tiền & Thanh toán ---
    @Column(name = "fare_amount", nullable = false)
    private BigDecimal fareAmount; // Tổng tiền cước

    @Column(name = "payment_method")
    private PaymentMethod paymentMethod; // CASH, WALLET

    @Column(name = "payment_status")
    private String paymentStatus; // UNPAID, PAID

    // --- Trạng thái & Lý do hủy ---
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TripStatus status = TripStatus.Requested;

    @Column(name = "cancel_reason")
    private String cancelReason; // Lý do hủy cuốc

    @Column(name = "cancelled_by")
    private String cancelledBy; // CUSTOMER hoặc DRIVER

    // --- Mốc thời gian (Timestamps) ---
    @Column(name = "requested_at")
    private LocalDateTime requestedAt; // Lúc khách đặt

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt; // Lúc tài xế nhận

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt; // Lúc tài xế đến nơi đón

    @Column(name = "started_at")
    private LocalDateTime startedAt; // Lúc bắt đầu chở (In-Progress)

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // Lúc trả khách xong

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt; // Lúc huỷ

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.requestedAt = LocalDateTime.now();
        if (this.paymentStatus == null) {
            this.paymentStatus = "UNPAID";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
