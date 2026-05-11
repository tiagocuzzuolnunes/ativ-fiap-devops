package br.com.fiap.skyrescue.service;

import br.com.fiap.skyrescue.domain.Drone;
import br.com.fiap.skyrescue.domain.DroneStatus;
import br.com.fiap.skyrescue.domain.Mission;
import br.com.fiap.skyrescue.domain.MissionStatus;
import br.com.fiap.skyrescue.dto.MissionRequest;
import br.com.fiap.skyrescue.dto.MissionResponse;
import br.com.fiap.skyrescue.exception.BusinessException;
import br.com.fiap.skyrescue.exception.ResourceNotFoundException;
import br.com.fiap.skyrescue.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final DroneService droneService;

    @Transactional(readOnly = true)
    public List<MissionResponse> findAll() {
        return missionRepository.findAll().stream().map(MissionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MissionResponse findById(Long id) {
        return MissionResponse.from(loadMission(id));
    }

    public MissionResponse create(MissionRequest request) {
        Mission mission = Mission.builder()
                .title(request.title())
                .description(request.description())
                .disasterType(request.disasterType())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status(MissionStatus.PLANNED)
                .build();

        if (request.droneId() != null) {
            Drone drone = droneService.loadDrone(request.droneId());
            if (drone.getStatus() != DroneStatus.AVAILABLE) {
                throw new BusinessException("Drone nao esta disponivel. Status atual: " + drone.getStatus());
            }
            drone.setStatus(DroneStatus.IN_MISSION);
            mission.setDrone(drone);
        }
        return MissionResponse.from(missionRepository.save(mission));
    }

    public MissionResponse updateStatus(Long id, MissionStatus status) {
        Mission mission = loadMission(id);
        mission.setStatus(status);
        if (status == MissionStatus.COMPLETED || status == MissionStatus.CANCELLED || status == MissionStatus.FAILED) {
            mission.setFinishedAt(LocalDateTime.now());
            if (mission.getDrone() != null) {
                mission.getDrone().setStatus(DroneStatus.AVAILABLE);
            }
        }
        return MissionResponse.from(missionRepository.save(mission));
    }

    public void delete(Long id) {
        Mission mission = loadMission(id);
        if (mission.getDrone() != null && mission.getStatus() == MissionStatus.IN_PROGRESS) {
            mission.getDrone().setStatus(DroneStatus.AVAILABLE);
        }
        missionRepository.delete(mission);
    }

    public Mission loadMission(Long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Missao nao encontrada: id=" + id));
    }
}
