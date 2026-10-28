package ra.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
