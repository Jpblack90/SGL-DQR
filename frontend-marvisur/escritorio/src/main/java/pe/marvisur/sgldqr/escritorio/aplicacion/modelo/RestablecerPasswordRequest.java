package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Petición de restablecimiento administrativo de contraseña (RF-03).
 * Por seguridad, la nueva contraseña nunca se expone en {@code toString()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RestablecerPasswordRequest(
        String nuevaPassword
) {
    @Override
    public String toString() {
        return "RestablecerPasswordRequest[nuevaPassword=[PROTEGIDO]]";
    }
}
