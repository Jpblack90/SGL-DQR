package pe.marvisur.sgldqr.api.dto.dashboard;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Rango temporal cerrado en el límite inferior y abierto en el límite superior [inicio, fin)
 * expresado con información de zona horaria (OffsetDateTime).
 */
public record RangoPeriodo(
        OffsetDateTime inicio,
        OffsetDateTime fin
) {
    public RangoPeriodo {
        Objects.requireNonNull(inicio, "La fecha/hora de inicio no puede ser nula");
        Objects.requireNonNull(fin, "La fecha/hora de fin no puede ser nula");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}
