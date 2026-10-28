package ra.demo.service;

import ra.demo.dto.request.DepositRequest;
import ra.demo.dto.request.WithdrawRequest;
import ra.demo.dto.response.WalletBalanceCheckResponse;
import ra.demo.dto.response.WalletResponse;
import ra.demo.dto.response.WalletTransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {
    // Xem số dư ví hiện tại.
    WalletResponse getMyWallet();
    // Nạp tiền vào ví.
    WalletResponse depositMoney(DepositRequest request);
    // Rút tiền khỏi ví.
    WalletResponse withdrawMoney(WithdrawRequest request);

    // Xem lịch sử biến động số dư.
    List<WalletTransactionResponse> getMyTransactionHistory();

    // 4. Kiểm tra số dư
    WalletBalanceCheckResponse checkBalanceDetail(Long customerId, BigDecimal amount);
}
