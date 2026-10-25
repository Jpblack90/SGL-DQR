package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * DTO para elementos de la bitácora de auditoría operativa y de seguridad (RF-03).
 */
@Schema(description = "Registro individual de bitácora de auditoría")
public record ItemAuditoriaDto(
        @Schema(description = "Identificador único del registro de auditoría (UUID)", example = "log-001")
        String idLog,

        @Schema(description = "Identificador del empleado que ejecutó la acción", example = "EMP-ADMIN-01")
        String idEmpleado,

        @Schema(description = "Nombres y apellidos del empleado", example = "Administrador Sistema Marvisur")
        String nombreEmpleado,

        @Schema(description = "Acción auditada ejecutada", example = "EMPLEADO_CREADO")
        String accion,

        @Schema(description = "Nombre de la tabla afectada en la base de datos", example = "empleados")
        String tablaAfectada,

        @Schema(description = "Identificador del registro afectado", example = "EMP-002")
        String idRegistroAfectado,

        @Schema(description = "Fecha y hora del evento con zona horaria (ISO 8601)", example = "2026-10-24T18:30:00-05:00")
        OffsetDateTime fechaHora,

        @Schema(description = "Dirección IP de origen de la petición", example = "192.168.1.100")
        String ipOrigen
) {
}
