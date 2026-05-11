package br.com.fiap.skyrescue.dto;

import br.com.fiap.skyrescue.domain.DisasterType;
import br.com.fiap.skyrescue.domain.Mission;
import br.com.fiap.skyrescue.domain.MissionStatus;

import java.time.LocalDateTime;

public record MissionResponse(
        Long id,
        String title,
        String description,
        DisasterType disasterType,
        MissionStatus status,
        Double latitude,
        Double longitude,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long droneId,
        int victimsCount
) {
    public static MissionResponse from(Mission mission) {
        return new MissionResponse(
                mission.getId(),
                mission.getTitle(),
                mission.getDescription(),
                mission.getDisasterType(),
                mission.getStatus(),
                mission.getLatitude(),
                mission.getLongitude(),
                mission.getStartedAt(),
                mission.getFinishedAt(),
                mission.getDrone() != null ? mission.getDrone().getId() : null,
                mission.getVictims() == null ? 0 : mission.getVictims().size()
        );
    }
}
