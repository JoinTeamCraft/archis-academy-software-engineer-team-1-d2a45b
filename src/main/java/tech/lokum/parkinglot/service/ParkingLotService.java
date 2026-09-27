package tech.lokum.parkinglot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.lokum.parkinglot.dto.ParkingLotDto;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.repository.ParkingLotRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository repository;

    public ParkingLot create(ParkingLotDto request) {

        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setName(request.getName());
        parkingLot.setAddress(request.getAddress());
        //parkingLot.setOperator(request.getOperator());


        return repository.save(parkingLot);
    }

    public List<ParkingLot> getAll() {

        return repository.findAll();
    }

    public ParkingLot getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new GlobalExceptionHandler.ResourceNotFoundException("Parking lot not found"));
    }

    public ParkingLot update(Long id, ParkingLotDto request) {
        ParkingLot parkingLot = getById(id);

        parkingLot.setName(request.getName());
        parkingLot.setAddress(request.getAddress());

        return repository.save(parkingLot);
    }

    public void delete(Long id) {
        ParkingLot parkingLot = getById(id);
        repository.delete(parkingLot);
    }

    public Long getAvailableSpots(Long id) {
        ParkingLot parkingLot = repository.findById(id)
                .orElseThrow(() ->
                        new GlobalExceptionHandler.ResourceNotFoundException("Parking lot not found"));

        return parkingLot.getParkingSpots()
                .stream()
                .filter(ParkingSpot::isActive)
                .count();

    }
}