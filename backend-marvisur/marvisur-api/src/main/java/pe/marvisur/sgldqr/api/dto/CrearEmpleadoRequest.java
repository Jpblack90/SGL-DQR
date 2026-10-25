package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Petición de alta de un nuevo colaborador (RF-03).
 */
@Schema(description = "Datos para el registro de un nuevo empleado")
public record CrearEmpleadoRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "^\\d{8}$", message = "El DNI debe tener exactamente 8 dígitos numéricos")
        @Schema(description = "DNI de 8 dígitos", example = "71829304")
        String dni,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(min = 2, max = 80, message = "Los nombres deben tener entre 2 y 80 caracteres")
        @Schema(description = "Nombres del empleado", example = "Juan")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(min = 2, max = 80, message = "Los apellidos deben tener entre 2 y 80 caracteres")
        @Schema(description = "Apellidos del empleado", example = "Pérez Mendoza")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato de correo no es válido")
        @Size(max = 120, message = "El correo no puede exceder 120 caracteres")
        @Schema(description = "Correo electrónico corporativo", example = "juan.perez@marvisur.pe")
        String correo,

        @NotBlank(message = "El identificador de rol es obligatorio")
        @Schema(description = "ID del rol asignado", example = "ROL-TN1-RECEPCION")
        String idRol,

        @NotBlank(message = "El identificador de sucursal es obligatorio")
        @Schema(description = "ID de la sucursal asignada", example = "SUC-LIM-01")
        String idSucursal,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Schema(description = "Contraseña inicial (mínimo 8 caracteres, al menos una letra y un número)", example = "ClaveSegura2026")
        String password
) {
}
