package tech.lokum.parkinglot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.ParkingLot;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ParkingLot} entities.
 */
@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    /**
     * Checks if a parking lot with the specified name already exists (case-insensitive).
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Checks if another parking lot has the specified name (excluding a specific id).
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Retrieves all active parking lots.
     */
    List<ParkingLot> findByActiveTrue();

    /**
     * Retrieves a page of active parking lots.
     */
    Page<ParkingLot> findByActiveTrue(Pageable pageable);

    /**
     * Finds an active parking lot by its primary key.
     */
    Optional<ParkingLot> findByIdAndActiveTrue(Long id);

    /**
     * Finds active parking lots matching a location keyword.
     */
    List<ParkingLot> findByLocationContainingIgnoreCaseAndActiveTrue(String location);
}
