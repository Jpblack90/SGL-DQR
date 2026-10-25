package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representación resumida de una agencia para origen o destino en rutas y viajes (RF-05).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgenciaResumenDto(
        String id,
        String nombreAgencia
) {
    @Override
    public String toString() {
        return nombreAgencia != null ? nombreAgencia : "";
    }
}
