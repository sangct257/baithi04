package ra.demo.initData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ra.demo.entity.Wallet;
import ra.demo.repository.WalletRepository;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitPayment implements CommandLineRunner {
    private final WalletRepository walletRepository;
    Long customerUserId = 2L;
    Long driverUserId = 3L;
    @Override
    public void run(String... args) throws Exception {
        // 1. Tạo ví cho Customer với số dư 1,000,000 VNĐ
        createWalletIfNotExist(customerUserId, "CUSTOMER", new BigDecimal("1000000.00"));

        // 2. Tạo ví cho Driver với số dư 500,000 VNĐ
        createWalletIfNotExist(driverUserId, "DRIVER", new BigDecimal("500000.00"));
    }

    private void createWalletIfNotExist(Long userId, String userType, BigDecimal initialBalance) {
        if (walletRepository.findByUserIdAndUserType(userId, userType).isEmpty()) {
            Wallet wallet = Wallet.builder()
                    .userId(userId)
                    .userType(userType)
                    .balance(initialBalance)
                    .currency("VND")
                    .build();

            walletRepository.save(wallet);
            log.info("--------> [PAYMENT SERVICE DB] Khởi tạo Ví thành công cho User ID: {} ({}) - Số dư: {} VNĐ",
                    userId, userType, initialBalance);
        }
    }
}
