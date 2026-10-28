package ra.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constains.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {
    private Long transactionId;     // mã giao dịch
    private Long tripId;            // mã chuyến
    private BigDecimal amount;      // tổng tiền
    private TransactionType type;   // Kiểu thanh toán
    private String status;          // trạng thái
    private String description;     // mô tả
    private LocalDateTime createdAt;
}