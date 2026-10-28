package ra.demo.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.constains.TransactionType;
import ra.demo.dto.request.DepositRequest;
import ra.demo.dto.request.WithdrawRequest;
import ra.demo.dto.response.WalletBalanceCheckResponse;
import ra.demo.dto.response.WalletResponse;
import ra.demo.dto.response.WalletTransactionResponse;
import ra.demo.entity.Wallet;
import ra.demo.entity.WalletTransaction;
import ra.demo.exception.BadRequestException;
import ra.demo.exception.ResourceNotFoundException;
import ra.demo.repository.WalletRepository;
import ra.demo.repository.WalletTransactionRepository;
import ra.demo.security.utils.SecurityUtils;
import ra.demo.service.WalletService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    // 1. Xem số dư ví hiện tại
    @Override
    public WalletResponse getMyWallet() {
        // Tự lấy userId từ Security Context trong Service
        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Tìm hoặc tạo mới Wallet (trả về kiểu Wallet)
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    String role = SecurityContextHolder.getContext().getAuthentication()
                            .getAuthorities().iterator().next().getAuthority();

                    String userType = role.contains("DRIVER") ? "DRIVER" : "CUSTOMER";

                    Wallet newWallet = new Wallet();
                    newWallet.setUserId(userId);
                    newWallet.setUserType(userType);
                    newWallet.setBalance(BigDecimal.ZERO);
                    newWallet.setCurrency("VND");
                    log.info("[TẠO VÍ MỚI] Đã khởi tạo ví cho User #{} ({})", userId, userType);
                    return walletRepository.save(newWallet);
                });
        // 2. Map từ Wallet sang WalletResponse rồi mới return
        return mapToWalletResponse(wallet);
    }

    // 2. Nạp tiền vào ví
    @Override
    @Transactional
    public WalletResponse depositMoney(DepositRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = getOrCreateWallet(userId);

        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = currentBalance.add(request.getAmount());

        wallet.setBalance(newBalance);
        Wallet savedWallet = walletRepository.save(wallet);

        // Ghi lịch sử giao dịch nạp tiền (Cộng tiền)
        String gateway = request.getPaymentGateway() != null ? request.getPaymentGateway() : "VNPAY";
        saveTransaction(savedWallet, null, request.getAmount(),
                TransactionType.TOP_UP, "Nạp tiền vào ví qua " + gateway);

        log.info("[NẠP TIỀN VÍ] User #{} | Nạp: {} VNĐ ({}) | Dư mới: {} VNĐ",
                userId, request.getAmount(), gateway, savedWallet.getBalance());
        return mapToWalletResponse(savedWallet);
    }

    // 3. Rút tiền khỏi ví
    @Override
    @Transactional
    public WalletResponse withdrawMoney(WithdrawRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = getOrCreateWallet(userId);

        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;

        // Kiểm tra đủ tiền rút không
        if (currentBalance.compareTo(request.getAmount()) < 0) {
            log.warn("[RÚT TIỀN THẤT BẠI] User #{}: Dư hiện tại ({} VNĐ) nhỏ hơn số tiền rút ({} VNĐ)",
                    userId, currentBalance, request.getAmount());
            throw new BadRequestException("Số dư ví không đủ để thực hiện rút tiền!");
        }

        BigDecimal newBalance = currentBalance.subtract(request.getAmount());
        wallet.setBalance(newBalance);
        Wallet savedWallet = walletRepository.save(wallet);

        // Ghi lịch sử giao dịch rút tiền (Sử dụng số âm)
        String desc = String.format("Rút tiền về NH %s (%s - %s)",
                request.getBankName(), request.getBankAccountNumber(), request.getBankAccountName());

        saveTransaction(savedWallet, null, request.getAmount().negate(),
                TransactionType.WITHDRAW, desc);

        log.info("[RÚT TIỀN VÍ] User #{} | Rút: {} VNĐ ({}) | Dư còn: {} VNĐ",
                userId, request.getAmount(), request.getBankName(), savedWallet.getBalance());
        return mapToWalletResponse(savedWallet);
    }

    // 4. Xem lịch sử biến động số dư
    @Override
    public List<WalletTransactionResponse> getMyTransactionHistory() {
        Long userId = SecurityUtils.getCurrentUserId();
        Wallet wallet = getOrCreateWallet(userId);

        List<WalletTransaction> transactions = transactionRepository.findByWalletWalletIdOrderByCreatedAtDesc(wallet.getWalletId());

        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    // 4. Kiểm tra số dư
    @Override
    public WalletBalanceCheckResponse checkBalanceDetail(Long customerId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return WalletBalanceCheckResponse.builder()
                    .enough(true)
                    .currentBalance(BigDecimal.ZERO)
                    .requiredAmount(BigDecimal.ZERO)
                    .missingAmount(BigDecimal.ZERO)
                    .build();
        }

        Wallet customerWallet = walletRepository.findByUserId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ví khách hàng ID: " + customerId));

        BigDecimal currentBalance = customerWallet.getBalance() != null ? customerWallet.getBalance() : BigDecimal.ZERO;
        boolean isEnough = currentBalance.compareTo(amount) >= 0;

        // Tính số tiền còn thiếu (nếu đủ thì missing = 0)
        BigDecimal missingAmount = isEnough ? BigDecimal.ZERO : amount.subtract(currentBalance);

        log.info("[CHECK SỐ DƯ] Khách #{} | Cần: {} VNĐ | Hiện có: {} VNĐ | Kết quả: {}",
                customerId, amount, currentBalance, isEnough ? "ĐỦ TIỀN" : "THIẾU " + missingAmount + " VNĐ");

        return WalletBalanceCheckResponse.builder()
                .enough(isEnough)
                .currentBalance(currentBalance)
                .requiredAmount(amount)
                .missingAmount(missingAmount)
                .build();
    }

    // --- HELPER METHODS ---

    private Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    String userType = "CUSTOMER"; // Default
                    try {
                        String role = SecurityContextHolder.getContext().getAuthentication()
                                .getAuthorities().iterator().next().getAuthority();
                        if (role != null && role.contains("DRIVER")) {
                            userType = "DRIVER";
                        }
                    } catch (Exception e) {
                        log.warn("[WALLET WARN] Không trích xuất được Role từ Security Context, gán mặc định CUSTOMER");
                    }

                    Wallet newWallet = Wallet.builder()
                            .userId(userId)
                            .userType(userType)
                            .balance(BigDecimal.ZERO)
                            .holdBalance(BigDecimal.ZERO)
                            .currency("VND")
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    private void saveTransaction(Wallet wallet, Long tripId, BigDecimal amount, TransactionType type, String description) {
        WalletTransaction tx = WalletTransaction.builder()
                .wallet(wallet)
                .tripId(tripId)
                .amount(amount)
                .type(type)
                .status("SUCCESS")
                .description(description)
                .build();
        transactionRepository.save(tx);
    }

    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .userType(wallet.getUserType())
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .transactionId(tx.getTransactionId())
                .tripId(tx.getTripId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
