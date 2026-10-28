package ra.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import ra.demo.constants.VehicleType;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private DriverProfile driverProfile;

    @Column(nullable = false, unique = true)
    private String licensePlate; // Biển số xe

    @Column(nullable = false)
    private String brand; // Hãng xe (Honda, Toyota...)

    @Column(nullable = false)
    private String model; // Dòng xe (Wave Alpha, Vios...)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType; // BIKE, CAR_4_SEAT...

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
