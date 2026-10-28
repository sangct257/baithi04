package ra.demo.security.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import ra.demo.security.principal.CustomUserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtProvider {
    @Value("${jwt-secret}")
    private String jwtSecret;
    @Value("${jwt-expire}")
    private long jwtExpiration;

    public String generateToken(CustomUserDetails customUserDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", customUserDetails.getId());
        claims.put("phoneNumber", customUserDetails.getPhoneNumber());
        claims.put("roles", customUserDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(customUserDetails.getId()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("JWT Token đã hết hạn!");
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            System.out.println("JWT Token không hợp lệ!");
        }
        return false;
    }

    public Long getUserIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.get("userId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.get("roles", List.class);
    }

}
