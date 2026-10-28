package ra.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ra.demo.dto.response.ApiResponse;

import java.math.BigDecimal;

@FeignClient(name = "payment-wallet-service", url = "${application.config.payment-service-url:http://localhost:8084}")
public interface PaymentClient {

    @GetMapping("/api/v1/wallets/check-balance")
    ApiResponse<Boolean> checkBalance(
            @RequestParam("userId") Long userId,
            @RequestParam("amount") BigDecimal amount
    );
}
