package br.com.fiap.skyrescue.service;

import br.com.fiap.skyrescue.domain.*;
import br.com.fiap.skyrescue.dto.MissionRequest;
import br.com.fiap.skyrescue.dto.MissionResponse;
import br.com.fiap.skyrescue.exception.BusinessException;
import br.com.fiap.skyrescue.repository.MissionRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    MissionRepository missionRepository;

    @Mock
    DroneService droneService;

    @InjectMocks
    MissionService missionService;

    @Test
    @DisplayName("deve criar missao sem drone associado")
    void shouldCreateMissionWithoutDrone() {
        MissionRequest request = new MissionRequest("Resgate Teste", "descricao", DisasterType.FLOOD, -23.5, -46.6, null);
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> {
            Mission m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        MissionResponse response = missionService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(MissionStatus.PLANNED);
        assertThat(response.droneId()).isNull();
    }

    @Test
    @DisplayName("deve alocar drone disponivel e colocar em IN_MISSION")
    void shouldAssignAvailableDrone() {
        Drone drone = Drone.builder().id(5L).serialNumber("SR-5").model("X").status(DroneStatus.AVAILABLE).batteryLevel(80).build();
        when(droneService.loadDrone(5L)).thenReturn(drone);
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> {
            Mission m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        MissionRequest request = new MissionRequest("Missao", "d", DisasterType.EARTHQUAKE, -23.0, -46.0, 5L);
        MissionResponse response = missionService.create(request);

        assertThat(response.droneId()).isEqualTo(5L);
        assertThat(drone.getStatus()).isEqualTo(DroneStatus.IN_MISSION);
    }

    @Test
    @DisplayName("deve recusar drone que nao esta AVAILABLE")
    void shouldRejectUnavailableDrone() {
        Drone drone = Drone.builder().id(5L).status(DroneStatus.CHARGING).build();
        when(droneService.loadDrone(5L)).thenReturn(drone);

        MissionRequest request = new MissionRequest("Missao", "d", DisasterType.EARTHQUAKE, -23.0, -46.0, 5L);

        assertThatThrownBy(() -> missionService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CHARGING");
    }

    @Test
    @DisplayName("deve liberar drone quando missao for concluida")
    void shouldReleaseDroneOnCompletion() {
        Drone drone = Drone.builder().id(2L).status(DroneStatus.IN_MISSION).build();
        Mission mission = Mission.builder()
                .id(1L)
                .title("m")
                .disasterType(DisasterType.WILDFIRE)
                .status(MissionStatus.IN_PROGRESS)
                .latitude(0.0)
                .longitude(0.0)
                .drone(drone)
                .build();
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> inv.getArgument(0));

        MissionResponse response = missionService.updateStatus(1L, MissionStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(MissionStatus.COMPLETED);
        assertThat(drone.getStatus()).isEqualTo(DroneStatus.AVAILABLE);
    }
}
