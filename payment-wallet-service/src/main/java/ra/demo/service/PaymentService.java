package ra.demo.service;

import java.math.BigDecimal;

public interface PaymentService {
    // 1. GIỮ TIỀN (HOLD) khi khách tạo Trip
    void holdCustomerWallet(Long customerId, Long tripId, BigDecimal fareAmount);
    // 2. TRỪ TIỀN THẬT (DEDUCT HOLD) & CỘNG VÍ TÀI XẾ khi Trip COMPLETED (WALLET)
    void processWalletPaymentOnComplete(Long customerId, Long driverId, Long tripId, BigDecimal fareAmount);
    // 2. Cộng 80% tiền thu nhập cho tài xế khi chuyến đi COMPLETED (WALLET)
    void creditDriverEarning(Long driverId, Long tripId, BigDecimal fareAmount);
    // 3. Trừ 20% hoa hồng trực tiếp ví tài xế khi chuyến đi COMPLETED (CASH)
    void deductDriverCommission(Long driverId, Long tripId, BigDecimal fareAmount);
    // 3. HOÀN TIỀN GIỮ (UNHOLD) khi Trip CANCELLED
    void refundHoldAmount(Long customerId, Long tripId, BigDecimal fareAmount);
}
