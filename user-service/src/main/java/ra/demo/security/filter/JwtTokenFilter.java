package ra.demo.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Kiểm tra Header X-Auth-User-Id từ Gateway trước (Nếu đi qua Gateway)
            String userIdHeader = request.getHeader("X-Auth-User-Id");
            String rolesHeader = request.getHeader("X-Auth-Roles");

            if (StringUtils.hasText(userIdHeader)) {
                setSecurityContext(Long.parseLong(userIdHeader), rolesHeader);
            }

            // 2. Dự phòng: Nếu gọi trực tiếp Port của User Service
            else {
                String jwt = getJwtFromRequest(request);
                if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                    Long userId = jwtProvider.getUserIdFromToken(jwt);
                    List<String> roles = jwtProvider.getRolesFromToken(jwt);
                    String rolesStr = roles != null ? String.join(",", roles) : "";

                    // Đọc thẳng thông tin từ JWT Token, KHÔNG QUERY DB LẠI!
                    setSecurityContext(userId, rolesStr);
                }
            }
        } catch (Exception e) {
            logger.error("Không thể xác thực người dùng: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    private void setSecurityContext(Long userId, String rolesHeader) {
        List<SimpleGrantedAuthority> authorities = List.of();
        if (StringUtils.hasText(rolesHeader)) {
            authorities = java.util.Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        // Lưu userId vào Principal thay vì lưu UserDetails (Giúp nhẹ RAM và cực nhanh)
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
