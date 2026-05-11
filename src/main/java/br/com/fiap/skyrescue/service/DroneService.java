package br.com.fiap.skyrescue.service;

import br.com.fiap.skyrescue.domain.Drone;
import br.com.fiap.skyrescue.domain.DroneStatus;
import br.com.fiap.skyrescue.dto.DroneRequest;
import br.com.fiap.skyrescue.dto.DroneResponse;
import br.com.fiap.skyrescue.exception.BusinessException;
import br.com.fiap.skyrescue.exception.ResourceNotFoundException;
import br.com.fiap.skyrescue.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DroneService {

    private final DroneRepository droneRepository;

    @Transactional(readOnly = true)
    public List<DroneResponse> findAll() {
        return droneRepository.findAll().stream().map(DroneResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DroneResponse findById(Long id) {
        return DroneResponse.from(loadDrone(id));
    }

    public DroneResponse create(DroneRequest request) {
        if (droneRepository.existsBySerialNumber(request.serialNumber())) {
            throw new BusinessException("Drone ja cadastrado com o serialNumber: " + request.serialNumber());
        }
        Drone drone = Drone.builder()
                .serialNumber(request.serialNumber())
                .model(request.model())
                .status(request.status() != null ? request.status() : DroneStatus.AVAILABLE)
                .batteryLevel(request.batteryLevel() != null ? request.batteryLevel() : 100)
                .lastLatitude(request.lastLatitude())
                .lastLongitude(request.lastLongitude())
                .build();
        return DroneResponse.from(droneRepository.save(drone));
    }

    public DroneResponse update(Long id, DroneRequest request) {
        Drone drone = loadDrone(id);
        drone.setModel(request.model());
        if (request.status() != null) {
            drone.setStatus(request.status());
        }
        if (request.batteryLevel() != null) {
            drone.setBatteryLevel(request.batteryLevel());
        }
        drone.setLastLatitude(request.lastLatitude());
        drone.setLastLongitude(request.lastLongitude());
        return DroneResponse.from(droneRepository.save(drone));
    }

    public void delete(Long id) {
        Drone drone = loadDrone(id);
        droneRepository.delete(drone);
    }

    public Drone loadDrone(Long id) {
        return droneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drone nao encontrado: id=" + id));
    }
}
