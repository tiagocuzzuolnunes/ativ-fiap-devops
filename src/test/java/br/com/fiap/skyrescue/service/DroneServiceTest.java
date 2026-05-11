package br.com.fiap.skyrescue.service;

import br.com.fiap.skyrescue.domain.Drone;
import br.com.fiap.skyrescue.domain.DroneStatus;
import br.com.fiap.skyrescue.dto.DroneRequest;
import br.com.fiap.skyrescue.dto.DroneResponse;
import br.com.fiap.skyrescue.exception.BusinessException;
import br.com.fiap.skyrescue.exception.ResourceNotFoundException;
import br.com.fiap.skyrescue.repository.DroneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DroneServiceTest {

    @Mock
    DroneRepository droneRepository;

    @InjectMocks
    DroneService droneService;

    private Drone sample;

    @BeforeEach
    void setup() {
        sample = Drone.builder()
                .id(1L)
                .serialNumber("SR-001")
                .model("SkyRescue Alpha")
                .status(DroneStatus.AVAILABLE)
                .batteryLevel(80)
                .build();
    }

    @Test
    @DisplayName("deve criar um novo drone quando o serial nao existir")
    void shouldCreateDrone() {
        DroneRequest request = new DroneRequest("SR-001", "SkyRescue Alpha", null, 80, null, null);
        when(droneRepository.existsBySerialNumber("SR-001")).thenReturn(false);
        when(droneRepository.save(any(Drone.class))).thenReturn(sample);

        DroneResponse result = droneService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.serialNumber()).isEqualTo("SR-001");
        assertThat(result.status()).isEqualTo(DroneStatus.AVAILABLE);
        verify(droneRepository).save(any(Drone.class));
    }

    @Test
    @DisplayName("deve falhar quando ja existir drone com o mesmo serialNumber")
    void shouldFailWhenSerialAlreadyExists() {
        DroneRequest request = new DroneRequest("SR-001", "SkyRescue Alpha", null, 80, null, null);
        when(droneRepository.existsBySerialNumber("SR-001")).thenReturn(true);

        assertThatThrownBy(() -> droneService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SR-001");

        verify(droneRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve lancar ResourceNotFoundException ao buscar drone inexistente")
    void shouldThrowWhenNotFound() {
        when(droneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> droneService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deve atualizar dados do drone existente")
    void shouldUpdateDrone() {
        DroneRequest request = new DroneRequest("SR-001", "SkyRescue Beta", DroneStatus.CHARGING, 50, -23.5, -46.6);
        when(droneRepository.findById(1L)).thenReturn(Optional.of(sample));
        when(droneRepository.save(any(Drone.class))).thenAnswer(inv -> inv.getArgument(0));

        DroneResponse result = droneService.update(1L, request);

        assertThat(result.model()).isEqualTo("SkyRescue Beta");
        assertThat(result.status()).isEqualTo(DroneStatus.CHARGING);
        assertThat(result.batteryLevel()).isEqualTo(50);
    }
}
