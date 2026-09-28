package tech.lokum.parkinglot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Vehicle} entities.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByLicensePlateIgnoreCase(String licensePlate);

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    List<Vehicle> findByUserId(Long userId);
}
