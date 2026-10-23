package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Resultado de una cotización de tarifa.
 */
public record CotizacionResultado(
        BigDecimal montoEstimado,
        String moneda
) {
}
