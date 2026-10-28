package ra.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ra.demo.entity.WalletTransaction;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletWalletIdOrderByCreatedAtDesc(Long walletId);
}