package br.com.fiap.skyrescue.dto;

import br.com.fiap.skyrescue.domain.DisasterType;
import jakarta.validation.constraints.*;

public record MissionRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 500) String description,
        @NotNull DisasterType disasterType,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        Long droneId
) {
}
