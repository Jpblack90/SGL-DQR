package pe.marvisur.sgldqr.api.dto;

import java.time.LocalDateTime;

/**
 * Evento en la línea de tiempo pública de tracking (RF-08).
 */
public record EventoTrackingPublicoDto(
        String estadoEvento,
        String sucursal,
        LocalDateTime fechaHora
) {
}
