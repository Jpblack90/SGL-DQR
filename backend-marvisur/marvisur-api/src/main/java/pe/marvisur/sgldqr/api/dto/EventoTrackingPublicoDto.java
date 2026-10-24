package pe.marvisur.sgldqr.api.dto;

import java.time.OffsetDateTime;

/**
 * Evento en la línea de tiempo pública de tracking (RF-08).
 * Se serializa con offset / ISO-8601 con zona para evitar desfases horarios en clientes web.
 */
public record EventoTrackingPublicoDto(
        String estadoEvento,
        String sucursal,
        OffsetDateTime fechaHora
) {
}
