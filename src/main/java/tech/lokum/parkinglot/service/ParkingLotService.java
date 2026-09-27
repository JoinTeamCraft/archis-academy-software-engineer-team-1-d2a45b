package tech.lokum.parkinglot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.Mapper;
import tech.lokum.parkinglot.dto.ParkingLotDto;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.repository.ParkingLotRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingLotService {

    private final ParkingLotRepository repository;
    private final Mapper mapper;

    public ParkingLotDto create(ParkingLotDto request) {

        ParkingLot parkingLot = mapper.mapToEntity(request);
        parkingLot.setId(null);
        ParkingLot saved = repository.save(parkingLot);
        return mapper.mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ParkingLotDto> getAll() {

        return repository.findAll()
                .stream()
                .map(mapper::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParkingLotDto getById(Long id) {
        ParkingLot parkingLot = repository.findById(id)
                .orElseThrow(() ->
                        new GlobalExceptionHandler.ResourceNotFoundException("Parking lot not found"));
        return mapper.mapToDto(parkingLot);
    }

    public ParkingLotDto update(Long id, ParkingLotDto request) {
        ParkingLot parkingLot = repository.findById(id)
                .orElseThrow(() ->
                        new GlobalExceptionHandler.ResourceNotFoundException("Parking lot not found"));

        parkingLot.setName(request.getName());
        parkingLot.setAddress(request.getAddress());

        ParkingLot updated = repository.save(parkingLot);
        return mapper.mapToDto(updated);
    }

    public void delete(Long id) {
        ParkingLot parkingLot = repository.findById(id)
                .orElseThrow(() ->
                        new GlobalExceptionHandler.ResourceNotFoundException("Parking lot not found"));
        repository.delete(parkingLot);
    }

    @Transactional(readOnly = true)
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