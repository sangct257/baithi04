package ra.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.constains.TransactionType;
import ra.demo.entity.Wallet;
import ra.demo.entity.WalletTransaction;
import ra.demo.exception.BadRequestException;
import ra.demo.exception.ResourceNotFoundException;
import ra.demo.repository.WalletRepository;
import ra.demo.repository.WalletTransactionRepository;
import ra.demo.service.PaymentService;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.20"); // Tỷ lệ hoa hồng 20%

    // 1. GIỮ TIỀN (HOLD WALLET)
    @Override
    @Transactional
    public void holdCustomerWallet(Long customerId, Long tripId, BigDecimal fareAmount) {
        // Tìm ví của Khách hàng theo customerId và type "CUSTOMER"
        Wallet customerWallet = walletRepository.findByUserIdAndUserType(customerId, "CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví khách hàng ID: " + customerId));

        BigDecimal currentBalance = customerWallet.getBalance() != null ? customerWallet.getBalance() : BigDecimal.ZERO;

        // Kiểm tra số dư khả dụng có đủ trả tiền cước chuyến đi không
        if (currentBalance.compareTo(fareAmount) < 0) {
            log.warn("[HOLD THẤT BẠI] Ví Khách #{}: Số dư khả dụng ({} VNĐ) không đủ giữ {} VNĐ", customerId, currentBalance, fareAmount);
            throw new BadRequestException("Số dư khả dụng không đủ để giữ tiền!");
        }

        // Bước 1: Trừ tiền từ số dư khả dụng (Balance)
        customerWallet.setBalance(currentBalance.subtract(fareAmount));

        // Bước 2: Cộng số tiền tương ứng vào số dư tạm giữ (HoldBalance)
        BigDecimal currentHold = customerWallet.getHoldBalance() != null ? customerWallet.getHoldBalance() : BigDecimal.ZERO;
        customerWallet.setHoldBalance(currentHold.add(fareAmount));

        walletRepository.save(customerWallet);

        // Lưu nhật ký giao dịch tài khoản: HOLD_FOR_TRIP
        saveTransaction(customerWallet, tripId, fareAmount,
                TransactionType.HOLD_FOR_TRIP, "Tạm giữ tiền cho chuyến đi #" + tripId);

        log.info("[GIỮ TIỀN VÍ KHÁCH] Khách #{} | Chuyến #{} | Số tiền: {} VNĐ | Dư còn: {} VNĐ | Tạm giữ: {} VNĐ",
                customerId, tripId, fareAmount, customerWallet.getBalance(), customerWallet.getHoldBalance());
    }

    // 2. XỬ LÝ THANH TOÁN VÍ KHI COMPLETED
    @Override
    @Transactional
    public void processWalletPaymentOnComplete(Long customerId, Long driverId, Long tripId, BigDecimal fareAmount) {
        // Bước 1: Trừ tiền tạm giữ (HoldBalance) của Khách hàng
        Wallet customerWallet = walletRepository.findByUserIdAndUserType(customerId, "CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví khách ID: " + customerId));

        BigDecimal currentHold = customerWallet.getHoldBalance() != null ? customerWallet.getHoldBalance() : BigDecimal.ZERO;
        customerWallet.setHoldBalance(currentHold.subtract(fareAmount));
        walletRepository.save(customerWallet);

        // Lưu biến động tài khoản khách: TRIP_PAYMENT (Trừ tiền hẳn)
        saveTransaction(customerWallet, tripId, fareAmount.negate(),
                TransactionType.TRIP_PAYMENT, "Thanh toán thành công chuyến đi #" + tripId);

        log.info("[THANH TOÁN VÍ HOÀN THÀNH] Chuyến #{} | Trừ tiền tạm giữ Khách #{}: -{} VNĐ (Hold còn: {} VNĐ)",
                tripId, customerId, fareAmount, customerWallet.getHoldBalance());

        // Bước 2: Cộng 80% thu nhập vào Ví Tài Xế
        creditDriverEarning(driverId, tripId, fareAmount);
    }

    // 3. CỘNG THU NHẬP CHO TÀI XẾ (+80%)
    @Override
    @Transactional
    public void creditDriverEarning(Long driverId, Long tripId, BigDecimal fareAmount) {
        Wallet driverWallet = walletRepository.findByUserIdAndUserType(driverId, "DRIVER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví tài xế ID: " + driverId));

        BigDecimal commission = fareAmount.multiply(COMMISSION_RATE); // 20%
        BigDecimal driverEarning = fareAmount.subtract(commission);   // 80%

        // Cộng 80% số tiền cước vào ví khả dụng của tài xế
        BigDecimal currentBalance = driverWallet.getBalance() != null ? driverWallet.getBalance() : BigDecimal.ZERO;
        driverWallet.setBalance(currentBalance.add(driverEarning));
        walletRepository.save(driverWallet);

        // Lưu lịch sử giao dịch: TRIP_EARNING
        saveTransaction(driverWallet, tripId, driverEarning,
                TransactionType.TRIP_EARNING, "Cộng 80% thu nhập chuyến đi #" + tripId);

        log.info("[CỘNG THU NHẬP TÀI XẾ] Tài xế #{} | Chuyến #{} | Thu nhập (+80%): {} VNĐ | Dư ví: {} VNĐ",
                driverId, tripId, driverEarning, driverWallet.getBalance());
    }

    // 4. TRỪ 20% HOA HỒNG TÀI XẾ KHI COMPLETED BẰNG TIỀN MẶT
    @Override
    @Transactional
    public void deductDriverCommission(Long driverId, Long tripId, BigDecimal fareAmount) {
        Wallet driverWallet = walletRepository.findByUserIdAndUserType(driverId, "DRIVER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví tài xế ID: " + driverId));

        // Tính 20% hoa hồng sàn được hưởng
        BigDecimal commission = fareAmount.multiply(COMMISSION_RATE); // 20%
        BigDecimal currentBalance = driverWallet.getBalance() != null ? driverWallet.getBalance() : BigDecimal.ZERO;

        // Trừ 20% tiền hoa hồng khỏi số dư khả dụng của tài xế
        driverWallet.setBalance(currentBalance.subtract(commission));
        walletRepository.save(driverWallet);

        // Lưu lịch sử giao dịch: COMMISSION_DEDUCTION
        saveTransaction(driverWallet, tripId, commission.negate(),
                TransactionType.COMMISSION_DEDUCTION, "Trừ 20% hoa hồng chuyến đi tiền mặt #" + tripId);

        log.info("[TRỪ HOA HỒNG TIỀN MẶT] Tài xế #{} | Chuyến #{} | Thu cước: {} VNĐ | Trừ hoa hồng (-20%): {} VNĐ | Dư ví: {} VNĐ",
                driverId, tripId, fareAmount, commission, driverWallet.getBalance());
    }

    // 5. HOÀN TIỀN TẠM GIỮ KHI HỦY CHUYẾN
    @Override
    @Transactional
    public void refundHoldAmount(Long customerId, Long tripId, BigDecimal fareAmount) {
        Wallet customerWallet = walletRepository.findByUserIdAndUserType(customerId, "CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví khách ID: " + customerId));

        BigDecimal currentHold = customerWallet.getHoldBalance() != null ? customerWallet.getHoldBalance() : BigDecimal.ZERO;
        BigDecimal currentBalance = customerWallet.getBalance() != null ? customerWallet.getBalance() : BigDecimal.ZERO;

        // Bước 1: Trừ tiền trong khoản tạm giữ (HoldBalance)
        customerWallet.setHoldBalance(currentHold.subtract(fareAmount));
        customerWallet.setBalance(currentBalance.add(fareAmount));
        walletRepository.save(customerWallet);

        // Lưu lịch sử giao dịch hoàn tiền: UNHOLD_TRIP
        saveTransaction(customerWallet, tripId, fareAmount,
                TransactionType.UNHOLD_TRIP, "Hoàn trả tiền tạm giữ do hủy chuyến #" + tripId);

        log.info("[HOÀN TIỀN GIỮ KHÁCH] Khách #{} | Chuyến bị hủy #{} | Hoàn: {} VNĐ | Dư ví: {} VNĐ",
                customerId, tripId, fareAmount, customerWallet.getBalance());
    }


    private void saveTransaction(Wallet wallet, Long tripId, BigDecimal amount, TransactionType transactionType, String description) {
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .tripId(tripId)
                .amount(amount)
                .type(transactionType)
                .status("SUCCESS")
                .description(description)
                .build();
        walletTransactionRepository.save(tx);
    }
}
