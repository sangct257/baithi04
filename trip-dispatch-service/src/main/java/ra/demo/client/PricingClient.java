package ra.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ra.demo.dto.request.CalculatePriceRequest;
import ra.demo.dto.response.ApiResponse;
import ra.demo.dto.response.PriceCalculationResponse;

// Point URL sang Cổng/Port của Pricing Service (VD: http://localhost:8083)
@FeignClient(name = "pricing-surge-service", url = "${application.config.pricing-service-url:http://localhost:8083}")
public interface PricingClient {
    @PostMapping("/api/v1/pricing/calculate")
    ApiResponse<PriceCalculationResponse> calculateFare(@RequestBody CalculatePriceRequest request);
}
