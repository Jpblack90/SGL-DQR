package pe.marvisur.sgldqr.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;

/**
 * Datos de entrada para un cliente remitente o destinatario con validación de documento (DNI 8 dígitos, RUC 11 dígitos).
 */
public record ClienteInputDto(
        @NotNull(message = "El tipo de documento es obligatorio")
        TipoDocumento tipoDocumento,

        @NotBlank(message = "El número de documento es obligatorio")
        String numeroDocumento,

        @NotBlank(message = "El nombre o razón social es obligatorio")
        String nombresRazonSocial,

        String telefono,

        @Email(message = "El formato de correo no es válido")
        String correo
) {
    @AssertTrue(message = "El número de documento debe tener exactamente 8 dígitos numéricos para DNI u 11 para RUC")
    public boolean isDocumentoValido() {
        if (tipoDocumento == null || numeroDocumento == null) {
            return false;
        }
        String limpio = numeroDocumento.trim();
        if (tipoDocumento == TipoDocumento.DNI) {
            return limpio.matches("^\\d{8}$");
        } else if (tipoDocumento == TipoDocumento.RUC) {
            return limpio.matches("^\\d{11}$");
        }
        return false;
    }
}
