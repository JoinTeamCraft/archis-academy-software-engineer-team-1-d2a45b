package tech.lokum.parkinglot.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lokum.parkinglot.dto.ParkingLotDto;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.service.ParkingLotService;

import java.util.List;

@RestController
@RequestMapping("/api/parking-lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    @PostMapping
    public ResponseEntity<ParkingLot> createParkingLot(
            @RequestBody ParkingLotDto request) {

        ParkingLot parkingLot = parkingLotService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(parkingLot);
    }

    @GetMapping
    public ResponseEntity<List<ParkingLot>> getAllParkingLots() {
        return ResponseEntity.ok(parkingLotService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingLot> getParkingLotById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                parkingLotService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingLot> updateParkingLot(@PathVariable Long id, @RequestBody ParkingLotDto request) {

        return ResponseEntity.ok(
                parkingLotService.update(id, request));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParkingLot(@PathVariable Long id) {

        parkingLotService.delete(id);

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{id}/available-spots")
    public ResponseEntity<Long> getAvailableSpots(@PathVariable Long id) {

        return ResponseEntity.ok(
                parkingLotService.getAvailableSpots(id));
    }
}
