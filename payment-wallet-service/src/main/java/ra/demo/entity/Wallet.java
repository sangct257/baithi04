package ra.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "user_type", nullable = false, length = 20)
    private String userType; // "DRIVER" hoặc "CUSTOMER"

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance; // số dư

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "hold_balance")
    private BigDecimal holdBalance = BigDecimal.ZERO;

    @PrePersist
    public void prePersist() {
        if (this.balance == null) this.balance = BigDecimal.ZERO;
        if (this.currency == null) this.currency = "VND";
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
