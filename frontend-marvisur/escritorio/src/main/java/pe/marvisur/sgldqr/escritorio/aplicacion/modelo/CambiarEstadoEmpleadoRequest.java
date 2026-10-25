package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Petición de cambio de estado de vigencia laboral (baja lógica o reactivación) (RF-03).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CambiarEstadoEmpleadoRequest(
        Boolean activo
) {
}
