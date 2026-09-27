package tech.lokum.parkinglot.dto;

import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.entity.*;

@Component
public class Mapper {
    public ParkingLot mapToEntity(ParkingLotDto dto) {
        if (dto == null) {
            return null;
        }
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setName(dto.getName());
        parkingLot.setAddress(dto.getAddress());
        return parkingLot;
    }

    public ParkingLotDto mapToDto(ParkingLot parkingLot) {
        if (parkingLot == null) {
            return null;
        }
        ParkingLotDto dto = new ParkingLotDto();
        dto.setId(parkingLot.getId());
        dto.setName(parkingLot.getName());
        dto.setAddress(parkingLot.getAddress());
        return dto;
    }

    public ParkingSpot mapToEntity(ParkingSpotDto dto, ParkingLot parkingLot) {
        if (dto == null) {
            return null;
        }
        ParkingSpot parkingSpot = new ParkingSpot();

        parkingSpot.setSpotNumber(dto.getSpotNumber());
        parkingSpot.setType(dto.getType());
        parkingSpot.setActive(dto.isActive());
        parkingSpot.setParkingLot(parkingLot);

        return parkingSpot;
    }

    public ParkingSpotDto mapToDto(ParkingSpot parkingSpot) {
        if (parkingSpot == null) {
            return null;
        }
        ParkingSpotDto dto = new ParkingSpotDto();

        dto.setId(parkingSpot.getId());
        dto.setSpotNumber(parkingSpot.getSpotNumber());
        dto.setType(parkingSpot.getType());
        dto.setActive(parkingSpot.isActive());

        if (parkingSpot.getParkingLot() != null) {
            dto.setParkingLotId(parkingSpot.getParkingLot().getId());
        }

        return dto;
    }
    public PaymentDto mapToDto(Payment payment) {
        if (payment == null) {
            return null;
        }
        PaymentDto dto = new PaymentDto();

        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setPaidAt(payment.getPaidAt());

        if (payment.getReservation() != null) {
            dto.setReservationId(payment.getReservation().getId());
        }

        return dto;
    }
    public Payment mapToEntity(PaymentDto dto, Reservation reservation) {
        if (dto == null) {
            return null;
        }
        Payment payment = new Payment();

        payment.setAmount(dto.getAmount());
        payment.setStatus(dto.getStatus());
        payment.setPaidAt(dto.getPaidAt());
        payment.setReservation(reservation);

        return payment;
    }
    public UserDto mapToDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());

        return dto;
    }
    public User mapToEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());

        return user;
    }
}
