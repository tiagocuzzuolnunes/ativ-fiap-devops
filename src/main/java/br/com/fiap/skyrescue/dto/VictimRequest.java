package br.com.fiap.skyrescue.dto;

import br.com.fiap.skyrescue.domain.VictimCondition;
import jakarta.validation.constraints.*;

public record VictimRequest(
        @Size(max = 150) String identification,
        VictimCondition condition,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double detectionConfidence,
        @Size(max = 500) String notes
) {
}
