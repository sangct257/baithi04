package ra.demo.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ra.demo.security.utils.JwtUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomHeaderAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 1. CẤU HÌNH BỎ QUA FILTER VỚI CÁC API PERMIT ALL / INTERNAL
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Bỏ qua lọc Header cho mọi request tới /nearby (kể cả có hay không có context path)
        return pathMatcher.match("/**/api/v1/location/nearby**", path)
                || path.contains("/nearby");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-Auth-User-Id");
        String rolesHeader = request.getHeader("X-Auth-Roles");

        // 1. Ưu tiên đọc Header do API Gateway đẩy xuống
        if (StringUtils.hasText(userIdHeader)) {
            setSecurityContext(Long.parseLong(userIdHeader), rolesHeader);
        }
        // 2. Dự phòng khi test trực tiếp port Payment Service (8084)
        else {
            String token = getJwtFromRequest(request);
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                Long userId = jwtUtils.extractUserId(token);
                List<String> roles = jwtUtils.extractRoles(token);
                String rolesStr = roles != null ? String.join(",", roles) : "";

                setSecurityContext(userId, rolesStr);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setSecurityContext(Long userId, String rolesHeader) {
        List<SimpleGrantedAuthority> authorities = List.of();
        if (StringUtils.hasText(rolesHeader)) {
            authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

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
