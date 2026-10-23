package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Parámetros para cotizar una tarifa de encomienda.
 */
public record CotizacionComando(
        String idSucursalOrigen,
        String idSucursalDestino,
        BigDecimal pesoKg,
        BigDecimal volumenM3
) {
}
