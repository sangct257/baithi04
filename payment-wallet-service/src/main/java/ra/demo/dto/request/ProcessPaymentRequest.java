package ra.demo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotNull(message = "Trip ID không được để trống")
    private Long tripId;

    @NotNull(message = "Customer ID không được để trống")
    private Long customerId;

    @NotNull(message = "Driver ID không được để trống")
    private Long driverId;

    @NotNull(message = "Số tiền không được để trống")
    private BigDecimal fareAmount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // "CASH", "WALLET"
}