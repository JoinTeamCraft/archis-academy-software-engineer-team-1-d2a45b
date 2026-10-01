package tech.lokum.parkinglot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tech.lokum.parkinglot.dto.ParkingSpotDto;
import tech.lokum.parkinglot.service.ParkingSpotService;

@RestController
@RequestMapping("/api/parking-spots")
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingSpotService parkingSpotService;

    @PostMapping
    public ResponseEntity<ParkingSpotDto> createParkingSpot(
            @RequestBody ParkingSpotDto request) {

        ParkingSpotDto parkingSpot = parkingSpotService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(parkingSpot);
    }
}