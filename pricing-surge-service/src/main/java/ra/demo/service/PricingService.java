package ra.demo.service;

import ra.demo.dto.request.CalculatePriceRequest;
import ra.demo.dto.response.PriceCalculationResponse;

public interface PricingService {
    // Tính toán khoảng cách, thời gian dự kiến và tổng cước phí chuyến đi
    PriceCalculationResponse calculateFare(CalculatePriceRequest calculatePriceRequest);

}
