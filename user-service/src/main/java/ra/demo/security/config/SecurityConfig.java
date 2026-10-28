package ra.demo.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ra.demo.security.exception.CustomAccessDeniedHandler;
import ra.demo.security.exception.JwtAuthenticationEntryPoint;
import ra.demo.security.filter.JwtTokenFilter;
import ra.demo.security.principal.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;
    private final JwtAuthenticationEntryPoint jwtEntryPoint; // Xử lý 401
    private final CustomAccessDeniedHandler accessDeniedHandler; // Xử lý 403
    private final CustomUserDetailsService userDetailsService; // 2. Inject thêm UserDetailsService vào đây

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // 1. Truyền userDetailsService trực tiếp vào Constructor
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);

        // 2. Set PasswordEncoder
        authProvider.setPasswordEncoder(passwordEncoder());

        // Mặc định hideUserNotFoundExceptions = true sẽ biến UsernameNotFoundException thành BadCredentialsException
        // Đặt false để phân biệt rõ lỗi "Không tìm thấy user" vs "Sai mật khẩu"
        // 3. Giữ nguyên cài đặt ẩn/hiện ngoại lệ nếu bạn muốn
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cho phép tất cả request OPTIONS đi qua không cần Auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/api/v1/auth/**").permitAll()

                        .requestMatchers(HttpMethod.PATCH,"/api/v1/users/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/users/customer/customer-deactivate").hasAnyRole("CUSTOMER","ADMIN")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/users/driver/driver-deactivate").hasAnyRole("DRIVER","ADMIN")

                        .requestMatchers(HttpMethod.PATCH,"/api/v1/driver/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,"/api/v1/driver/profile").hasAnyRole("DRIVER","ADMIN")
                        .requestMatchers(HttpMethod.PATCH,"/api/v1/driver/status").hasAnyRole("DRIVER","ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtEntryPoint) // Bắt lỗi 401 (Chưa đăng nhập / Token hết hạn)
                        .accessDeniedHandler(accessDeniedHandler) // Bắt lỗi 403 (Đã đăng nhập nhưng không đủ quyền)
                )
                // Đăng ký Provider đã cấu hình ở trên vào Spring Security
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}