package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.math.BigDecimal;

/**
 * Resumen de totales de encomiendas registradas en el período de consulta (RF-02).
 */
public record TotalesDashboardDto(
        long registradas,
        long entregadas,
        long enRuta,
        PendientesPagoDto pendientesDePago,
        BigDecimal recaudado
) {
}
