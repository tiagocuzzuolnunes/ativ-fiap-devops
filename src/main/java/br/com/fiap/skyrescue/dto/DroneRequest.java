package br.com.fiap.skyrescue.dto;

import br.com.fiap.skyrescue.domain.DroneStatus;
import jakarta.validation.constraints.*;

public record DroneRequest(
        @NotBlank @Size(max = 50) String serialNumber,
        @NotBlank @Size(max = 100) String model,
        DroneStatus status,
        @Min(0) @Max(100) Integer batteryLevel,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double lastLatitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double lastLongitude
) {
}
