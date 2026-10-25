package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta para visualización y consulta de personal operativo (RF-03).
 * No expone contraseñas ni hashes de seguridad.
 */
@Schema(description = "Datos de personal operativo de Marvisur")
public record EmpleadoDto(
        @Schema(description = "Identificador único del empleado (UUID)", example = "EMP-ADMIN-01")
        String idEmpleado,

        @Schema(description = "DNI de 8 dígitos", example = "45871234")
        String dni,

        @Schema(description = "Nombres del colaborador", example = "Carlos")
        String nombres,

        @Schema(description = "Apellidos del colaborador", example = "Quispe Mamani")
        String apellidos,

        @Schema(description = "Correo electrónico corporativo", example = "carlos.quispe@marvisur.pe")
        String correo,

        @Schema(description = "Identificador del rol asignado", example = "ROL-TN3-ADMIN")
        String idRol,

        @Schema(description = "Nombre legible del rol", example = "Administrador")
        String nombreRol,

        @Schema(description = "Identificador de la sucursal asignada", example = "SUC-LIM-01")
        String idSucursal,

        @Schema(description = "Nombre de la agencia asignada", example = "Agencia Lima Central")
        String nombreSucursal,

        @Schema(description = "Estado de vigencia laboral (activo = true, baja lógica = false)", example = "true")
        Boolean estado
) {
}
