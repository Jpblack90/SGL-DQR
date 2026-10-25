package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Detalle de una encomienda en el manifiesto de carga de un viaje (RF-05).
 * Cumple con la directiva estricta de CERO datos personales.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManifiestoEncomiendaDto(
        String codigoComprobante,
        String estadoLogistico,
        String nombreAgenciaDestino,
        BigDecimal pesoKg
) {
}
