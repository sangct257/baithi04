package ra.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DepositRequest {
    @NotNull(message = "Số tiền nạp không được để trống")
    @DecimalMin(value = "10000", message = "Số tiền nạp tối thiểu là 10,000 VNĐ")
    private BigDecimal amount;

    private String paymentGateway; // Ví dụ: "VNPay", "Momo", "ZaloPay", "BANK_TRANSFER"
}
