package tech.lokum.parkinglot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.Mapper;
import tech.lokum.parkinglot.dto.ParkingSpotDto;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.repository.ParkingLotRepository;
import tech.lokum.parkinglot.repository.ParkingSpotRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final Mapper mapper;

    public ParkingSpotDto create(ParkingSpotDto request) {

        ParkingLot parkingLot = parkingLotRepository.findById(
                        request.getParkingLotId())
                .orElseThrow(() ->
                        new RuntimeException("Parking Lot not found"));

        ParkingSpot parkingSpot = mapper.mapToEntity(request, parkingLot);
        parkingSpot.setId(null);
        ParkingSpot saved = parkingSpotRepository.save(parkingSpot);
        return mapper.mapToDto(saved);
    }
}