package tech.lokum.parkinglot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tech.lokum.parkinglot.dto.CreateParkingLotRequest;
import tech.lokum.parkinglot.dto.ParkingLotResponse;
import tech.lokum.parkinglot.dto.UpdateParkingLotRequest;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.ParkingLotRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingLotServiceTest {

    @Mock
    private ParkingLotRepository parkingLotRepository;

    @InjectMocks
    private ParkingLotService parkingLotService;

    private ParkingLot lot1;
    private ParkingLot lot2;

    @BeforeEach
    void setUp() {
        lot1 = new ParkingLot("Central Garage", "123 Main St", 150);
        lot1.setId(1L);

        lot2 = new ParkingLot("North Lot", "456 North Ave", 80);
        lot2.setId(2L);
    }

    @Test
    @DisplayName("getAllParkingLots should return all active parking lots mapped to DTOs")
    void shouldReturnAllParkingLots() {
        when(parkingLotRepository.findByActiveTrue()).thenReturn(List.of(lot1, lot2));

        List<ParkingLotResponse> result = parkingLotService.getAllParkingLots();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Central Garage");
        assertThat(result.get(0).location()).isEqualTo("123 Main St");
        assertThat(result.get(0).capacity()).isEqualTo(150);

        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).name()).isEqualTo("North Lot");
    }

    @Test
    @DisplayName("getAllParkingLots with pageable should return paginated DTOs")
    void shouldReturnPaginatedParkingLots() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<ParkingLot> page = new PageImpl<>(List.of(lot1), pageable, 2);
        when(parkingLotRepository.findByActiveTrue(pageable)).thenReturn(page);

        Page<ParkingLotResponse> result = parkingLotService.getAllParkingLots(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getContent().get(0).name()).isEqualTo("Central Garage");
    }

    @Test
    @DisplayName("getParkingLotById should return lot when found")
    void shouldReturnParkingLotById() {
        when(parkingLotRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(lot1));

        ParkingLotResponse result = parkingLotService.getParkingLotById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Central Garage");
    }

    @Test
    @DisplayName("getParkingLotById should throw ResourceNotFoundException when lot does not exist")
    void shouldThrowExceptionWhenNotFound() {
        when(parkingLotRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parkingLotService.getParkingLotById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("ParkingLot not found");
    }

    @Test
    @DisplayName("createParkingLot should save and return new lot")
    void shouldCreateParkingLot() {
        CreateParkingLotRequest request = new CreateParkingLotRequest("South Plaza", "789 South Blvd", 200);
        when(parkingLotRepository.existsByNameIgnoreCase("South Plaza")).thenReturn(false);

        ParkingLot saved = new ParkingLot("South Plaza", "789 South Blvd", 200);
        saved.setId(3L);
        when(parkingLotRepository.save(any(ParkingLot.class))).thenReturn(saved);

        ParkingLotResponse result = parkingLotService.createParkingLot(request);

        assertThat(result.id()).isEqualTo(3L);
        assertThat(result.name()).isEqualTo("South Plaza");
        assertThat(result.capacity()).isEqualTo(200);
    }

    @Test
    @DisplayName("createParkingLot should throw ConflictException when lot name already exists")
    void shouldThrowConflictWhenCreatingDuplicateName() {
        CreateParkingLotRequest request = new CreateParkingLotRequest("Central Garage", "Different St", 50);
        when(parkingLotRepository.existsByNameIgnoreCase("Central Garage")).thenReturn(true);

        assertThatThrownBy(() -> parkingLotService.createParkingLot(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("updateParkingLot should modify attributes and save")
    void shouldUpdateParkingLot() {
        UpdateParkingLotRequest request = new UpdateParkingLotRequest("Updated Central", "125 Main St", 180, true);
        when(parkingLotRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(lot1));
        when(parkingLotRepository.existsByNameIgnoreCaseAndIdNot("Updated Central", 1L)).thenReturn(false);
        when(parkingLotRepository.save(any(ParkingLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingLotResponse result = parkingLotService.updateParkingLot(1L, request);

        assertThat(result.name()).isEqualTo("Updated Central");
        assertThat(result.location()).isEqualTo("125 Main St");
        assertThat(result.capacity()).isEqualTo(180);
    }

    @Test
    @DisplayName("deleteParkingLot should set active to false")
    void shouldSoftDeleteParkingLot() {
        when(parkingLotRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(lot1));

        parkingLotService.deleteParkingLot(1L);

        assertThat(lot1.isActive()).isFalse();
        verify(parkingLotRepository).save(lot1);
    }
}
