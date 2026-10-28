package ra.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ra.demo.constants.TripStatus;
import ra.demo.entity.Trip;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {
    boolean existsByCustomerIdAndStatusIn(Long customerId, Collection<TripStatus> activeStatuses);

    Optional<Trip> findByIdAndCustomerId(Long tripId, Long customerId);

    List<Trip> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Trip> findByIdAndDriverId(Long tripId, Long driverId);

    List<Trip> findByDriverIdOrderByCreatedAtDesc(Long driverId);
}
