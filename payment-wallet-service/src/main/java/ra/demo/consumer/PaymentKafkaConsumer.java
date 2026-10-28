package ra.demo.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ra.demo.constains.PaymentMethod;
import ra.demo.constains.TripStatus;
import ra.demo.event.RideRequestEvent;
import ra.demo.event.TripStatusEvent;
import ra.demo.service.PaymentService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaConsumer {

    private final PaymentService paymentService;

    /**
     * Lắng nghe Topic 'ride-request-topic' ngầm từ Kafka.
     * Ngay khi Khách hàng đặt xe thành công, hàm này tự động được kích hoạt!
     */
    @KafkaListener(topics = "ride-request-topic", groupId = "payment-wallet-group")
    public void handleRideRequestEvent(RideRequestEvent event) {
        log.info("[KAFKA CONSUME] Payment Service nhận RideRequestEvent cho Trip #{}", event.getTripId());

        // Kiểm tra phương thức thanh toán: Nếu là WALLET mới thực hiện giữ tiền
        if (PaymentMethod.WALLET.equals(event.getPaymentMethod()) ){
            try {
                log.info("[AUTOPAY WALLET] Đang thực hiện Tạm giữ {} VNĐ cho Khách #{}", event.getAmount(), event.getCustomerId());
                paymentService.holdCustomerWallet(event.getCustomerId(), event.getTripId(), event.getAmount());
                log.info("[AUTOPAY SUCCESS] Đã tạm giữ tiền thành công cho Trip #{}", event.getTripId());
            } catch (Exception e) {
                log.error("[AUTOPAY ERROR] Lỗi tạm giữ tiền ví cho Trip #{}: {}", event.getTripId(), e.getMessage());
                // Tại đây bạn có thể bắn 1 Event "trip-cancelled-topic" để báo Trip Service hủy chuyến vì không đủ tiền!
            }
        } else {
            log.info("[PAYMENT CASH] Trip #{} dùng Tiền mặt, không cần Hold ví.", event.getTripId());
        }
    }

    /**
     * Lắng nghe biến động trạng thái chuyến đi để Trừ tiền / Hoàn tiền / Trừ hoa hồng tài xế.
     */
    @KafkaListener(topics = "trip-status-topic", groupId = "payment-wallet-group")
    public void handleTripStatusEvent(TripStatusEvent event) {
        log.info("[KAFKA CONSUME] Payment Service nhận TripStatusEvent cho Trip #{}, Trạng thái: {}", event.getTripId(), event.getTripStatus());

        if (TripStatus.Completed.equals(event.getTripStatus())) {
            if (PaymentMethod.WALLET.equals(event.getPaymentMethod())) {
                // Trừ khoản tạm giữ của khách + Cộng 80% cho ví tài xế
                paymentService.processWalletPaymentOnComplete(
                        event.getCustomerId(), event.getDriverId(), event.getTripId(), event.getFareAmount());
            } else if (PaymentMethod.CASH.equals(event.getPaymentMethod())) {
                // Trừ 20% hoa hồng trực tiếp từ ví tài xế
                paymentService.deductDriverCommission(
                        event.getDriverId(), event.getTripId(), event.getFareAmount());
            }
        } else if (TripStatus.Cancelled.equals(event.getTripStatus())) {
            if (PaymentMethod.WALLET.equals(event.getPaymentMethod())) {
                // Hoàn lại tiền tạm giữ về ví khả dụng cho khách
                paymentService.refundHoldAmount(
                        event.getCustomerId(), event.getTripId(), event.getFareAmount());
            }
        }
    }
}
