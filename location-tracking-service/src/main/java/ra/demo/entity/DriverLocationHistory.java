package ra.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_location_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long driverId;
    private Long tripId; // Mang tripId nếu đang chở khách, null nếu đang rảnh
    private Double latitude;
    private Double longitude;
    private String vehicleType;
    private String h3Index; // Ô lục giác Uber H3 (Resolution 8)

    private LocalDateTime createdAt;
}