package br.com.fiap.skyrescue.dto;

import br.com.fiap.skyrescue.domain.Victim;
import br.com.fiap.skyrescue.domain.VictimCondition;

import java.time.LocalDateTime;

public record VictimResponse(
        Long id,
        String identification,
        VictimCondition condition,
        Double latitude,
        Double longitude,
        Double detectionConfidence,
        LocalDateTime detectedAt,
        String notes,
        Long missionId
) {
    public static VictimResponse from(Victim victim) {
        return new VictimResponse(
                victim.getId(),
                victim.getIdentification(),
                victim.getCondition(),
                victim.getLatitude(),
                victim.getLongitude(),
                victim.getDetectionConfidence(),
                victim.getDetectedAt(),
                victim.getNotes(),
                victim.getMission() != null ? victim.getMission().getId() : null
        );
    }
}
