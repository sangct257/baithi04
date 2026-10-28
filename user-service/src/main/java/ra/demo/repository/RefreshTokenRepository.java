package ra.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ra.demo.entity.RefreshToken;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    void deleteByPhoneNumber(String phoneNumber);

    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    Optional<RefreshToken> findByPhoneNumber(String phoneNumber);
}
