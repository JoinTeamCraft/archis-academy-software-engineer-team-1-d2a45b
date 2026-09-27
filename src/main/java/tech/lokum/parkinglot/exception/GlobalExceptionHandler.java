package tech.lokum.parkinglot.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParkingLotAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleParkingLotExists(
            ParkingLotAlreadyExistsException ex) {

        return Map.of("message", ex.getMessage());
    }


    public class ParkingLotAlreadyExistsException extends RuntimeException {

        public ParkingLotAlreadyExistsException(String name) {
            super("Parking lot already exists: " + name);
        }
    }


    public static class ResourceNotFoundException extends RuntimeException {

        public ResourceNotFoundException(String message) {
            super(message);
        }
    }


}