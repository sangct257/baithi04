package ra.demo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt-secret}")
    private String SECRET_KEY;

    public String extractPhoneNumber(String token){
        return extractAllClaims(token).get("phoneNumber", String.class);
    }

    public Long extractUserId(String token){
        return extractAllClaims(token).get("userId", Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token){
        return extractAllClaims(token).get("roles", List.class);
    }


    public Date extractExpiration(String token){
        return extractAllClaims(token).getExpiration();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Boolean validateToken(String token) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(secretKey).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            log.error("Gateway JWT Error: Token đã hết hạn.");
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Gateway JWT Error: Chữ ký Token không hợp lệ hoặc bị thay đổi.");
        }
        return false;
    }
}
