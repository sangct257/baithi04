package ra.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class DriverLoginRequest {
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phoneNumber;
    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải đúng 6 chữ số")
    @Pattern(regexp = "^[0-9]{6}$", message = "Mã OTP chỉ được chứa chữ số")
    private String otp;
}
