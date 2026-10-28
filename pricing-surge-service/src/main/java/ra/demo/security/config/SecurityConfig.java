package ra.demo.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ra.demo.security.exception.CustomAccessDeniedHandler;
import ra.demo.security.exception.CustomAuthenticationEntryPoint;
import ra.demo.security.filter.HeaderAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Cho phép dùng Annotation @PreAuthorize("hasAuthority(...)") tại Controller/Service
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Tắt CSRF vì Microservices dùng REST API & Token Stateless, không dùng Session/Cookie
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Cấu hình Quản lý Session là STATELESS (Không lưu trạng thái đăng nhập phía Server)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. Khai báo quy tắc PHÂN QUYỀN (Authorization) theo Endpoint API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/calculate").permitAll()

                        // --- API CẤP QUẢN LÝ BẢNG GIÁ: DÀNH RIÊNG CHO ADMIN ---
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pricing/rules/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/seed-rules").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pricing/mock-surge").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/pricing/check-redis").hasRole("ADMIN")

                        // Yêu cầu TẤT CẢ các API còn lại trong service phải được xác thực (phải có Header từ Gateway)
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint) // Xử lý lỗi 401 (Thiếu Token)
                        .accessDeniedHandler(customAccessDeniedHandler)             // Xử lý lỗi 403 (Sai Role)
                )
                // 4. Đặt Filter kiểm tra Header của chúng ta VÀO TRƯỚC Filter xác thực mặc định của Spring
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}