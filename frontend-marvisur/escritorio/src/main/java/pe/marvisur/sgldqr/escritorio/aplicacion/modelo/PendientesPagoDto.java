package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Indicador de encomiendas con pago pendiente en el período de consulta (RF-02).
 */
public record PendientesPagoDto(
        long cantidad,
        BigDecimal monto
) {
}
