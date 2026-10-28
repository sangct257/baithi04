package ra.demo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ra.demo.dto.request.DepositRequest;
import ra.demo.dto.request.WithdrawRequest;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.WalletBalanceCheckResponse;
import ra.demo.dto.response.WalletResponse;
import ra.demo.dto.response.WalletTransactionResponse;
import ra.demo.service.WalletService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // GET /api/v1/wallets/balance: Xem số dư ví hiện tại
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyBalance() {
        WalletResponse response = walletService.getMyWallet();
        return new ResponseEntity(new ApiResponse<>(
                true,
                "Lấy thông tin ví thành công!",
                response,
                null,
                HttpStatus.OK
        ),HttpStatus.OK);
    }

    // POST /api/v1/wallets/deposit: Nạp tiền vào ví
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<WalletResponse>> depositMoney(@Valid @RequestBody DepositRequest request) {
        WalletResponse response = walletService.depositMoney(request);
        return new ResponseEntity(new ApiResponse<>(
                true,
                "Nạp tiền vào ví thành công!",
                response,
                null,
                HttpStatus.OK
        ),HttpStatus.OK);
    }

    // POST /api/v1/wallets/withdraw: Rút tiền khỏi ví
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<WalletResponse>> withdrawMoney(@Valid @RequestBody WithdrawRequest request) {
        WalletResponse response = walletService.withdrawMoney(request);
        return new ResponseEntity(new ApiResponse<>(
                true,
                "Yêu cầu rút tiền thành công!",
                response,
                null,
                HttpStatus.OK
        ),HttpStatus.OK);
    }

    // GET /api/v1/wallets/transactions: Xem lịch sử biến động số dư
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionResponse>>> getMyTransactionHistory() {
        List<WalletTransactionResponse> response = walletService.getMyTransactionHistory();
        return new ResponseEntity(new ApiResponse<>(
                true,
                "Lấy lịch sử giao dịch thành công!",
                response,
                null,
                HttpStatus.OK
        ),HttpStatus.OK);
    }

    @GetMapping("/check-balance")
    public ResponseEntity<ApiResponse<Boolean>> checkBalance(
            @RequestParam Long userId,
            @RequestParam BigDecimal amount) {
        WalletBalanceCheckResponse response = walletService.checkBalanceDetail(userId, amount);

        return new ResponseEntity(new ApiResponse<>(
                true,
                "Kiểm tra số dư thành công",
                response,
                null,
                HttpStatus.OK
        ),HttpStatus.OK);
    }
}