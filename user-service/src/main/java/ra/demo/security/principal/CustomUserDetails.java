package ra.demo.security.principal;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ra.demo.constants.AccountStatus;

import java.util.Collection;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CustomUserDetails implements UserDetails {
    private Long id;
    private String phoneNumber;
    private String password;
    private String fullName;
    private String email;
    private AccountStatus status;
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != AccountStatus.CUSTOMER_LOCKED
                && this.status != AccountStatus.DRIVER_LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status != AccountStatus.CUSTOMER_DELETED
                && this.status != AccountStatus.DRIVER_DELETED;
    }
}
