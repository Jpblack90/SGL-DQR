package pe.marvisur.sgldqr.api.dto;

import java.math.BigDecimal;

/**
 * Respuesta pública con el cálculo estimado de la tarifa (RF-10).
 */
public record CotizacionResponse(
        BigDecimal montoEstimado,
        String moneda
) {
}
