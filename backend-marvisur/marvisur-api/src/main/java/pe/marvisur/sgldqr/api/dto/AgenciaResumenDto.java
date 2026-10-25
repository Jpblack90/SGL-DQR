package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representación resumida de una agencia para origen o destino en rutas y viajes.
 */
@Schema(description = "Datos resumidos de una agencia o sucursal")
public record AgenciaResumenDto(

        @Schema(description = "Identificador único de la sucursal", example = "suc-lima-01")
        String id,

        @Schema(description = "Nombre oficial de la agencia", example = "Agencia Lima Central")
        String nombreAgencia
) {
}
