package pe.marvisur.sgldqr.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Respuesta pública para la consulta de rastreo/tracking de encomiendas (RF-08).
 * Diseñado con Fricción Cero y Privacidad: nombres enmascarados y sin PII sensible.
 */
public record RastreoPublicoDto(
        String codigoComprobante,
        String estadoLogistico,
        String estadoPago,
        BigDecimal montoTotal,
        String sucursalOrigen,
        String ciudadOrigen,
        String sucursalDestino,
        String ciudadDestino,
        String nombreRemitenteEnmascarado,
        String nombreDestinatarioEnmascarado,
        List<EventoTrackingPublicoDto> lineaDeTiempo
) {
}
