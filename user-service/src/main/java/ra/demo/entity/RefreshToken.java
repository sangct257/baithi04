package ra.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;

    @Column(nullable = false, unique = true)
    private String refreshToken;

    private Instant expiryDate;

    private Boolean invoke = false; // Đánh dấu token đã bị thu hồi (khi Logout hoặc bị Admin khóa)
}
