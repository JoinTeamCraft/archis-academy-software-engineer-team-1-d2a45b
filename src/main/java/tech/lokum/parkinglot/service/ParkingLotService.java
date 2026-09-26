package tech.lokum.parkinglot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.CreateParkingLotRequest;
import tech.lokum.parkinglot.dto.ParkingLotResponse;
import tech.lokum.parkinglot.dto.UpdateParkingLotRequest;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.ParkingLotRepository;

import java.util.List;

/**
 * Service managing parking lots, including CRUD operations, availability queries, and pagination.
 */
@Service
public class ParkingLotService {

    private static final Logger log = LoggerFactory.getLogger(ParkingLotService.class);

    private final ParkingLotRepository parkingLotRepository;

    public ParkingLotService(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    /**
     * Retrieves all active parking lots as a list.
     *
     * @return list of active parking lots
     */
    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getAllParkingLots() {
        log.debug("Fetching all active parking lots");
        return parkingLotRepository.findByActiveTrue()
            .stream()
            .map(ParkingLotResponse::fromEntity)
            .toList();
    }

    /**
     * Retrieves a paginated slice of active parking lots.
     *
     * @param pageable pagination details (page, size, sort)
     * @return page of parking lots
     */
    @Transactional(readOnly = true)
    public Page<ParkingLotResponse> getAllParkingLots(Pageable pageable) {
        log.debug("Fetching page {} of parking lots with size {}", pageable.getPageNumber(), pageable.getPageSize());
        return parkingLotRepository.findByActiveTrue(pageable)
            .map(ParkingLotResponse::fromEntity);
    }

    /**
     * Retrieves a single active parking lot by ID.
     *
     * @param id parking lot primary key
     * @return parking lot details
     * @throws ResourceNotFoundException if no active parking lot exists with the given ID
     */
    @Transactional(readOnly = true)
    public ParkingLotResponse getParkingLotById(Long id) {
        log.debug("Fetching parking lot with id {}", id);
        return parkingLotRepository.findByIdAndActiveTrue(id)
            .map(ParkingLotResponse::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("ParkingLot", "id", id));
    }

    /**
     * Creates a new parking lot after validating uniqueness.
     *
     * @param request creation payload
     * @return newly created parking lot
     * @throws ConflictException if a parking lot with the same name already exists
     */
    @Transactional
    public ParkingLotResponse createParkingLot(CreateParkingLotRequest request) {
        log.info("Creating parking lot with name '{}' at '{}'", request.name(), request.location());
        if (parkingLotRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException(String.format("Parking lot with name '%s' already exists", request.name()));
        }

        ParkingLot parkingLot = new ParkingLot(request.name(), request.location(), request.capacity());
        ParkingLot saved = parkingLotRepository.save(parkingLot);
        return ParkingLotResponse.fromEntity(saved);
    }

    /**
     * Updates an existing parking lot.
     *
     * @param id parking lot ID to update
     * @param request fields to update
     * @return updated parking lot
     * @throws ResourceNotFoundException if the parking lot does not exist
     * @throws ConflictException if the updated name collides with another parking lot
     */
    @Transactional
    public ParkingLotResponse updateParkingLot(Long id, UpdateParkingLotRequest request) {
        log.info("Updating parking lot with id {}", id);
        ParkingLot parkingLot = parkingLotRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("ParkingLot", "id", id));

        if (request.name() != null && !request.name().isBlank()) {
            if (parkingLotRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
                throw new ConflictException(String.format("Parking lot with name '%s' already exists", request.name()));
            }
            parkingLot.setName(request.name());
        }

        if (request.location() != null && !request.location().isBlank()) {
            parkingLot.setLocation(request.location());
        }

        if (request.capacity() != null) {
            parkingLot.setCapacity(request.capacity());
        }

        if (request.active() != null) {
            parkingLot.setActive(request.active());
        }

        ParkingLot updated = parkingLotRepository.save(parkingLot);
        return ParkingLotResponse.fromEntity(updated);
    }

    /**
     * Soft-deletes a parking lot by marking it inactive.
     *
     * @param id parking lot ID
     * @throws ResourceNotFoundException if the parking lot does not exist
     */
    @Transactional
    public void deleteParkingLot(Long id) {
        log.info("Deleting parking lot with id {}", id);
        ParkingLot parkingLot = parkingLotRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("ParkingLot", "id", id));

        parkingLot.setActive(false);
        parkingLotRepository.save(parkingLot);
    }
}
