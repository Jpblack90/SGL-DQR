package pe.marvisur.sgldqr.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * DTO para la consulta de rutas terrestres interprovinciales (RF-05).
 */
@Schema(description = "Datos de una ruta interprovincial de transporte")
public record RutaDto(

        @Schema(description = "Identificador de la ruta", example = "ruta-01")
        @JsonProperty("id")
        String id,

        @Schema(description = "Agencia de origen de la ruta")
        @JsonProperty("origen")
        AgenciaResumenDto origen,

        @Schema(description = "Agencia de destino de la ruta")
        @JsonProperty("destino")
        AgenciaResumenDto destino,

        @Schema(description = "Distancia en kilómetros entre origen y destino", example = "1010.50")
        @JsonProperty("distanciaKm")
        BigDecimal distanciaKm
) {
    public String idRuta() {
        return id;
    }
}
