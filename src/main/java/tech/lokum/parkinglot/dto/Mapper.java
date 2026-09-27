package tech.lokum.parkinglot.dto;

import tech.lokum.parkinglot.entity.*;

public class Mapper {
    public ParkingSpot mapToEntity(ParkingSpotDto dto, ParkingLot parkingLot) {
        ParkingSpot parkingSpot = new ParkingSpot();

        parkingSpot.setId(dto.getId());
        parkingSpot.setSpotNumber(dto.getSpotNumber());
        parkingSpot.setType(dto.getType());
        parkingSpot.setActive(dto.isActive());
        parkingSpot.setParkingLot(parkingLot);

        return parkingSpot;
    }
    public ParkingSpotDto mapToDto(ParkingSpot parkingSpot) {
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
        Payment payment = new Payment();

        payment.setId(dto.getId());
        payment.setAmount(dto.getAmount());
        payment.setStatus(dto.getStatus());
        payment.setPaidAt(dto.getPaidAt());
        payment.setReservation(reservation);

        return payment;
    }
    public UserDto mapToDto(User user) {

        UserDto dto = new UserDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());

        return dto;
    }
    public User mapToEntity(UserDto dto) {

        User user = new User();

        user.setId(dto.getId());
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());

        return user;
    }
}
