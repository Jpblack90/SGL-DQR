package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de restablecimiento de contraseña por el administrador (RF-03).
 */
@Schema(description = "Petición de restablecimiento de contraseña")
public record RestablecerPasswordRequest(
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
        @Schema(description = "Nueva contraseña (mínimo 8 caracteres, al menos una letra y un número)", example = "NuevaClave2026")
        String nuevaPassword
) {
}
