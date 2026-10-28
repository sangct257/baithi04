package ra.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ra.demo.entity.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByLicensePlateAndDriverProfileIdNot(String licensePlate, Long userId);
}
