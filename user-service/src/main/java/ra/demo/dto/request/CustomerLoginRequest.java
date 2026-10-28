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
public class CustomerLoginRequest {
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng")
    private String phoneNumber;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 4, message = "Mật khẩu phải từ 4 ký tự trở lên")
    private String password;
}
