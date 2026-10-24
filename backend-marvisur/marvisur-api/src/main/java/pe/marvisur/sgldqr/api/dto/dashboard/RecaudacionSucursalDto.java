package pe.marvisur.sgldqr.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Recaudación y montos pendientes agrupados por sucursal de origen (RF-02).
 */
@Schema(description = "Métricas de recaudación por sucursal de origen en el período")
public record RecaudacionSucursalDto(
        @Schema(description = "Identificador de la sucursal de origen", example = "SUC-LIM-01")
        String idSucursal,

        @Schema(description = "Nombre comercial de la agencia", example = "Agencia Central")
        String nombreAgencia,

        @Schema(description = "Monto acumulado efectivamente cobrado", example = "1200.00")
        BigDecimal recaudado,

        @Schema(description = "Monto acumulado pendiente de cobro", example = "50.00")
        BigDecimal pendiente
) {
}
