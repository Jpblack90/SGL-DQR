package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;

/**
 * Petición para cambiar el estado operativo de un viaje (TN3 Administrador o TN4 Chofer asignado).
 */
@Schema(description = "Petición para modificar el estado de un viaje programado")
public record CambiarEstadoViajeRequest(

        @Schema(description = "Nuevo estado del viaje (EN_RUTA o FINALIZADO)", example = "EN_RUTA")
        @NotNull(message = "El nuevo estado del viaje es obligatorio")
        EstadoViaje estado
) {
}
