package pe.marvisur.sgldqr.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Indicador de encomiendas con pago pendiente en el período de consulta (RF-02).
 */
@Schema(description = "Desglose de encomiendas pendientes de pago")
public record PendientesPagoDto(
        @Schema(description = "Cantidad total de encomiendas con pago pendiente", example = "2")
        long cantidad,

        @Schema(description = "Monto monetario acumulado pendiente de cobro", example = "90.00")
        BigDecimal monto
) {
}
