package pe.marvisur.sgldqr.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Resumen de totales de encomiendas registradas en el período de consulta (RF-02).
 */
@Schema(description = "Totales agregados de encomiendas registradas en el período")
public record TotalesDashboardDto(
        @Schema(description = "Total de encomiendas registradas en el período", example = "42")
        long registradas,

        @Schema(description = "Total de encomiendas que alcanzaron el estado ENTREGADO", example = "35")
        long entregadas,

        @Schema(description = "Total de encomiendas en estado EN_RUTA", example = "4")
        long enRuta,

        @Schema(description = "Detalle de encomiendas registradas en el período con pago pendiente")
        PendientesPagoDto pendientesDePago,

        @Schema(description = "Monto total recaudado por encomiendas registradas y pagadas en el período", example = "1850.50")
        BigDecimal recaudado
) {
}
