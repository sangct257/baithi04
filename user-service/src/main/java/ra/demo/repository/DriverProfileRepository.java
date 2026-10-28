package ra.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ra.demo.entity.DriverProfile;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    boolean existsByDriverLicenseNumberAndIdNot(String driverLicenseNumber, Long id);
    boolean existsByIdentityCardAndIdNot(String identityCard, Long id);
}
