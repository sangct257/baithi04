package ra.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import ra.demo.constants.DriverStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile {
    @Id
    private Long id; // Dùng chung ID với User (Mối quan hệ 1-1)

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String driverLicenseNumber; // Số bằng lái

    @Column(nullable = false, unique = true)
    private String identityCard; // Số CCCD/CMND

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriverStatus status = DriverStatus.OFFLINE; // Online/Offline/Busy[cite: 1]

    private Double rating = 5.0;

    private Boolean isApproved = false; // Quản trị viên đã duyệt chưa

    @OneToOne(mappedBy = "driverProfile", cascade = CascadeType.ALL)
    private Vehicle vehicle;

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
