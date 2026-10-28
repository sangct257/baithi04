package ra.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ra.demo.constants.PaymentMethod;
import ra.demo.constants.VehicleType;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CreateTripRequest {
    // Bắt buộc nhập tên địa chỉ/địa điểm ping
    @NotBlank(message = "Địa chỉ điểm đón không được để trống")
    private String pickupAddress;

    // Bắt buộc nhập tên địa chỉ/địa điểm ping
    @NotBlank(message = "Địa chỉ điểm trả không được để trống")
    private String dropoffAddress;

    @NotNull(message = "Loại xe không được để trống")
    private VehicleType vehicleType;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;
}
