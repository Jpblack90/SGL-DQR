package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Petición de cambio de estado de vigencia (baja lógica o reactivación) (RF-03).
 */
@Schema(description = "Petición de baja lógica o reactivación de un empleado")
public record CambiarEstadoEmpleadoRequest(
        @NotNull(message = "El campo activo es obligatorio")
        @Schema(description = "true para activar/reactivar, false para baja lógica", example = "false")
        Boolean activo
) {
}
