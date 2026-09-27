package tech.lokum.parkinglot.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.lokum.parkinglot.dto.ParkingSpotDto;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.repository.ParkingLotRepository;
import tech.lokum.parkinglot.repository.ParkingSpotRepository;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingLotRepository parkingLotRepository;

    public ParkingSpot create(ParkingSpotDto request) {

        ParkingLot parkingLot = parkingLotRepository.findById(
                        request.getParkingLotId())
                .orElseThrow(() ->
                        new RuntimeException("Parking Lot not found"));

        ParkingSpot parkingSpot = new ParkingSpot();

        parkingSpot.setSpotNumber(request.getSpotNumber());
        parkingSpot.setType(request.getType());
        parkingSpot.setParkingLot(parkingLot);
        parkingSpot.setActive(request.isActive());

        return parkingSpotRepository.save(parkingSpot);
    }
}