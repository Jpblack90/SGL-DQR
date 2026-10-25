package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;

/**
 * Registro de bitácora de auditoría operativa y de seguridad (RF-03).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemAuditoriaDto(
        String idLog,
        String idEmpleado,
        String nombreEmpleado,
        String accion,
        String tablaAfectada,
        String idRegistroAfectado,
        OffsetDateTime fechaHora,
        String ipOrigen
) {
}
