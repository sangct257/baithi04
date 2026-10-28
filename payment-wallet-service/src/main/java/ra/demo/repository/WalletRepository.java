package ra.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ra.demo.entity.Wallet;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserIdAndUserType(Long userId, String userType);

    Optional<Wallet> findByUserId(Long userId);
}