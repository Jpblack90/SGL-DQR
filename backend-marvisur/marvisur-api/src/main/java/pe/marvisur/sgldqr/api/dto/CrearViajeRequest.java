package pe.marvisur.sgldqr.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Petición para programar un nuevo viaje interprovincial (TN3 Administrador).
 */
@Schema(description = "Petición para programar un nuevo viaje interprovincial")
public record CrearViajeRequest(

        @Schema(description = "Identificador de la ruta asociada", example = "ruta-01")
        @NotBlank(message = "El identificador de ruta es obligatorio")
        String idRuta,

        @Schema(description = "Identificador del vehículo asignado", example = "veh-01")
        @NotBlank(message = "El identificador de vehículo es obligatorio")
        String idVehiculo,

        @Schema(description = "Identificador del chofer asignado (debe ser TN4_CHOFER activo)", example = "emp-04")
        @NotBlank(message = "El identificador de chofer es obligatorio")
        String idChofer,

        @Schema(description = "Fecha y hora estimada de salida (ISO 8601)", example = "2026-10-25T20:00:00-05:00")
        @NotNull(message = "La fecha y hora de salida es obligatoria")
        @JsonDeserialize(using = OffsetDateTimeFlexibleDeserializer.class)
        OffsetDateTime fechaSalida,

        @Schema(description = "Fecha y hora estimada de llegada (ISO 8601, posterior a la salida)", example = "2026-10-26T12:00:00-05:00")
        @NotNull(message = "La fecha y hora de llegada estimada es obligatoria")
        @JsonDeserialize(using = OffsetDateTimeFlexibleDeserializer.class)
        OffsetDateTime fechaLlegadaEstimada
) {
}
