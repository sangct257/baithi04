package ra.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ra.demo.entity.DriverLocationHistory;

import java.time.LocalDateTime;

@Repository
public interface DriverLocationHistoryRepository extends JpaRepository<DriverLocationHistory, Long> {
    @Transactional
    @Modifying
    @Query("DELETE FROM DriverLocationHistory d WHERE d.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
