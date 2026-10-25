package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición para la modificación de datos de un empleado (RF-03).
 * El DNI no es modificable.
 */
@Schema(description = "Datos modificables del empleado")
public record ActualizarEmpleadoRequest(
        @NotBlank(message = "Los nombres son obligatorios")
        @Size(min = 2, max = 80, message = "Los nombres deben tener entre 2 y 80 caracteres")
        @Schema(description = "Nombres del empleado", example = "Juan Carlos")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(min = 2, max = 80, message = "Los apellidos deben tener entre 2 y 80 caracteres")
        @Schema(description = "Apellidos del empleado", example = "Pérez Mendoza")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato de correo no es válido")
        @Size(max = 120, message = "El correo no puede exceder 120 caracteres")
        @Schema(description = "Correo electrónico corporativo", example = "juancarlos.perez@marvisur.pe")
        String correo,

        @NotBlank(message = "El identificador de rol es obligatorio")
        @Schema(description = "ID del rol asignado", example = "ROL-TN2-ALMACEN")
        String idRol,

        @NotBlank(message = "El identificador de sucursal es obligatorio")
        @Schema(description = "ID de la sucursal asignada", example = "SUC-LIM-01")
        String idSucursal
) {
}
