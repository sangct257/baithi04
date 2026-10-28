package ra.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class JwtResponse {
    private String phoneNumber;
    private String fullName;
    private String email;
    private String status;
    private Collection<? extends GrantedAuthority> authorities;
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String type = "Bearer";
}
