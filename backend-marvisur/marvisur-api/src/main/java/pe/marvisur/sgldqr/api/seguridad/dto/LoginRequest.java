package pe.marvisur.sgldqr.api.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Petición de inicio de sesión con correo y contraseña.
 */
public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato de correo no es válido")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
