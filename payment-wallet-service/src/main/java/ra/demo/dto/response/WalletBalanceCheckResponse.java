    package ra.demo.dto.response;

    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.math.BigDecimal;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public class WalletBalanceCheckResponse {
        private boolean enough;             // true nếu đủ tiền, false nếu thiếu
        private BigDecimal currentBalance;  // Số dư hiện tại trong ví
        private BigDecimal requiredAmount;  // Số tiền cước chuyến đi
        private BigDecimal missingAmount;   // Số tiền còn thiếu (nếu đủ tiền thì = 0)
    }