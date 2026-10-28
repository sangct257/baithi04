package ra.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResponse {
    private Double distanceKm; // khoảng cách (km)
    private Integer durationMinutes; // thời gian dự tính (phut)
    private BigDecimal fareAmount; // Tổng cước
    private Double surgeMultiplier; // Hệ số nhân

    // Trả về tọa độ thực tế đã định vị được từ địa chỉ ping
    private Double pickupLat; // Vĩ độ đón
    private Double pickupLng; // Kinh độ đón
    private Double dropoffLat; // Vĩ độ trả
    private Double dropoffLng; // Kinh độ trả
}
