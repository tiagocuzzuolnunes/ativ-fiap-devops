package br.com.fiap.skyrescue.dto;

import br.com.fiap.skyrescue.domain.Drone;
import br.com.fiap.skyrescue.domain.DroneStatus;

import java.time.LocalDateTime;

public record DroneResponse(
        Long id,
        String serialNumber,
        String model,
        DroneStatus status,
        Integer batteryLevel,
        Double lastLatitude,
        Double lastLongitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DroneResponse from(Drone drone) {
        return new DroneResponse(
                drone.getId(),
                drone.getSerialNumber(),
                drone.getModel(),
                drone.getStatus(),
                drone.getBatteryLevel(),
                drone.getLastLatitude(),
                drone.getLastLongitude(),
                drone.getCreatedAt(),
                drone.getUpdatedAt()
        );
    }
}
